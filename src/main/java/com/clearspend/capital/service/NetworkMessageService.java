package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.repository.NetworkMessageRepository;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.clearspend.capital.service.AccountService.HoldRecord;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkMessageService {

  private final NetworkMessageRepository networkMessageRepository;

  private final AccountService accountService;
  private final AccountActivityService accountActivityService;
  private final AllocationService allocationService;
  private final CardService cardService;

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Transactional
  public void processNetworkMessage(NetworkCommon common) {
    // update common with data we have locally
    CardRecord cardRecord = cardService.getCardByExternalRef(common.getCardExternalRef());

    // if the card isn't found, set decline bits and return to caller
    if (cardRecord == null) {
      common.getDeclineReasons().add(DeclineReason.CARD_NOT_FOUND);
      common.setPostDecline(true);
      log.error("failed to find card with externalRef: " + common.getCardExternalRef());
      return;
    }

    common.setBusinessId(cardRecord.card().getBusinessId());
    common.setCard(cardRecord.card());
    common.setAccount(cardRecord.account());
    common.setAllocation(
        allocationService.retrieveAllocation(
            common.getBusinessId(), cardRecord.card().getAllocationId()));

    // TODO(kuchlein): lookup local merchantName table to retrieve logo
    // common.getAccountActivity().setMerchantLogoUrl();

    // TODO(kuchlein): lookup local merchantAddress table to retrieve lat/long
    // common.getAccountActivity().setMerchantLatitude();
    // common.getAccountActivity().setMerchantLongitude();

    // actually process the network message from Stripe
    switch (common.getNetworkMessageType()) {
      case AUTH_REQUEST -> processAuthorizationRequest(common);
      case AUTH_CREATED -> processAuthorizationCreated(common);
      case AUTH_UPDATED -> processAuthorizationUpdated(common);
      case TRANSACTION_CREATED -> processTransactionCreated(common);
      default -> throw new IllegalArgumentException(
          "invalid networkMessageType " + common.getNetworkMessageType());
    }

    // store any data that resulted from processing the network message from Stripe
    NetworkMessage networkMessage =
        new NetworkMessage(
            cardRecord.card().getBusinessId(),
            cardRecord.card().getAllocationId(),
            common.getNetworkMessageGroupId(),
            common.getNetworkMessageType(),
            common.getRequestedAmount(),
            common.getMerchantName(),
            common.getMerchantAddress(),
            common.getMerchantNumber(),
            common.getMerchantCategoryCode(),
            common.getExternalRef());

    // TODO(kuchlein): determine why we can't simply use the request as the field type
    // networkMessage.setRequest(common.getRequest());
    try {
      networkMessage.setRequest(objectMapper.writeValueAsString(common.getRequest()));
    } catch (JsonProcessingException e) {
      log.error("failed to serialize common.request", e);
    }

    // card may be null in the case of Stripe sending us transactions for cards that we've not
    // issued
    // TODO(kuchlein): determine if Stripe handle this for us or not
    if (common.getCard() != null) {
      networkMessage.setCardId(common.getCard().getId());
    }

    if (common.isPostHold()) {
      HoldRecord holdRecord =
          accountService.recordNetworkHold(
              common.getAccount(), common.getRequestedAmount(), OffsetDateTime.now().plusDays(2));
      networkMessage.setHoldId(holdRecord.hold().getId());
      common.setHold(holdRecord.hold());
      common.getAccountActivityDetails().setActivityTime(holdRecord.hold().getCreated());
      common.getAccountActivityDetails().setHideAfter(holdRecord.hold().getExpirationDate());
      accountActivityService.recordNetworkHoldAccountActivity(common, holdRecord.hold());
      log.debug(
          "networkMessage {} hold {} (available {} / ledger {})",
          networkMessage.getId(),
          networkMessage.getHoldId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    if (common.isPostAdjustment()) {
      AdjustmentRecord adjustmentRecord =
          accountService.recordNetworkAdjustment(common.getAccount(), common.getRequestedAmount());
      common.setAdjustment(adjustmentRecord.adjustment());
      networkMessage.setAdjustmentId(adjustmentRecord.adjustment().getId());
      common
          .getAccountActivityDetails()
          .setActivityTime(adjustmentRecord.adjustment().getCreated());
      accountActivityService.recordNetworkAdjustmentAccountActivity(
          common, adjustmentRecord.adjustment());
      log.debug(
          "networkMessage {} adjustment {} (available {} / ledger {})",
          networkMessage.getId(),
          networkMessage.getAdjustmentId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    if (common.isPostDecline()) {
      // TODO(kuchlein): do we need a separate decline table?
      common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.DECLINED);
      common.getAccountActivityDetails().setActivityTime(OffsetDateTime.now());
      accountActivityService.recordNetworkDeclineAccountActivity(common);
      log.debug(
          "networkMessage {} declined (available {} / ledger {})",
          networkMessage.getId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    common.setNetworkMessage(networkMessageRepository.save(networkMessage));
  }

  private void processAuthorizationUpdated(NetworkCommon common) {}

  private void processAuthorizationCreated(NetworkCommon common) {}

  private void processAuthorizationRequest(NetworkCommon common) {
    common.getRequestedAmount().ensureNegative();

    if (!common.getCard().getStatus().equals(CardStatus.ACTIVE)) {
      common.getDeclineReasons().add(DeclineReason.INVALID_CARD_STATUS);
      common.setPostDecline(true);
      return;
    }

    // account has no money at all
    if (common.getAccount().getAvailableBalance().isLessThanOrEqualToZero()) {
      common.getDeclineReasons().add(DeclineReason.INSUFFICIENT_FUNDS);
      common.setPostDecline(true);
      return;
    }

    // account has insufficient funds and partial approval isn't an option
    if (!common.isAllowPartialApproval()) {
      if (common
          .getAccount()
          .getAvailableBalance()
          .add(common.getRequestedAmount())
          .isLessThanZero()) {
        common.getDeclineReasons().add(DeclineReason.INSUFFICIENT_FUNDS);
        common.setPostDecline(true);
        return;
      }
      common.setApprovedAmount(common.getRequestedAmount());
    } else {
      common.setApprovedAmount(
          Amount.min(common.getAccount().getAvailableBalance(), common.getRequestedAmount().abs()));
    }

    common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.PENDING);
    common.setPostHold(true);
  }

  private void processTransactionCreated(NetworkCommon common) {
    if (common
        .getAccount()
        .getAvailableBalance()
        .add(common.getRequestedAmount())
        .isGreaterThanOrEqualZero()) {
      common.setPostDecline(true);
      return;
    }
    common.setApprovedAmount(common.getRequestedAmount());

    common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.APPROVED);
    common.setPostAdjustment(true);

    // lookup authorization, release funds and update account
    Optional<NetworkMessage> networkMessageOptional =
        networkMessageRepository.findByExternalRef(common.getStripeAuthorizationExternalRef());
    if (networkMessageOptional.isPresent()) { // TODO(kuchlein): determine if this is always true
      NetworkMessage networkMessage = networkMessageOptional.get();
      common.setNetworkMessageGroupId(networkMessage.getNetworkMessageGroupId());
      Optional<Hold> holdOptional =
          common.getAccount().getHolds().stream()
              .filter(hold -> hold.getId().equals(networkMessage.getHoldId()))
              .findFirst();
      if (holdOptional.isPresent()) {
        Hold hold = holdOptional.get();
        hold.setStatus(HoldStatus.RELEASED);
        common.getUpdatedHolds().add(hold);
        common.getAccount().recalculateAvailableBalance();
        accountActivityService.recordNetworkHoldReleaseAccountActivity(hold);
      }
    }
  }

  private void processServiceFee(NetworkCommon common) {}
}
