package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.data.model.Decline;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.repository.NetworkMessageRepository;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.clearspend.capital.service.AccountService.HoldRecord;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
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

    // TODO(kuchlein): lookup local merchantName table to retrieve logo (needs to be done async and
    //    potentially in a async batch job)
    // common.getAccountActivity().setMerchantLogoUrl();

    // TODO(kuchlein): lookup local merchantAddress table to retrieve lat/long (needs to be done
    //    async and potentially in a async batch job)
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
          "networkMessage {} for {} hold {} (available {} / ledger {})",
          networkMessage.getId(),
          common.getRequestedAmount(),
          networkMessage.getHoldId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    if (common.isPostAdjustment()) {
      AdjustmentRecord adjustmentRecord =
          accountService.recordNetworkAdjustment(common.getAccount(), common.getRequestedAmount());
      common.setAdjustmentRecord(adjustmentRecord);
      networkMessage.setAdjustmentId(adjustmentRecord.adjustment().getId());
      common
          .getAccountActivityDetails()
          .setActivityTime(adjustmentRecord.adjustment().getCreated());
      accountActivityService.recordNetworkAdjustmentAccountActivity(
          common, adjustmentRecord.adjustment());
      log.debug(
          "networkMessage {} for {} adjustment {} (available {} / ledger {})",
          networkMessage.getId(),
          common.getRequestedAmount(),
          networkMessage.getAdjustmentId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    if (common.isPostDecline()) {
      Decline decline =
          accountService.recordNetworkDecline(
              common.getAccount(),
              common.getCard(),
              common.getRequestedAmount(),
              common.getDeclineReasons());
      common.setDecline(decline);
      networkMessage.setDeclineId(decline.getId());
      common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.DECLINED);
      common.getAccountActivityDetails().setActivityTime(OffsetDateTime.now());
      accountActivityService.recordNetworkDeclineAccountActivity(common);
      log.warn(
          "networkMessage {} for {} declined (available {} / ledger {})",
          networkMessage.getId(),
          common.getRequestedAmount(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    common.setNetworkMessage(networkMessageRepository.save(networkMessage));
  }

  private void processAuthorizationUpdated(NetworkCommon common) {
    NetworkMessage networkMessage = getPriorAuthNetworkMessage(common.getExternalRef());
    common.setNetworkMessageGroupId(networkMessage.getNetworkMessageGroupId());
    // TODO(kuchlein): handle the case when the hold amount changes
  }

  private void processAuthorizationCreated(NetworkCommon common) {
    NetworkMessage networkMessage = getPriorAuthNetworkMessage(common.getExternalRef());
    common.setNetworkMessageGroupId(networkMessage.getNetworkMessageGroupId());
  }

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

    // TODO(kuchlein): assess spending limits on card
    // if (over limit)
    // {
    //    common.setApprovedAmount(Amount.of(amount.getCurrency()));
    //    common.setPostDecline(true);
    //    return;
    // }

    common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.PENDING);
    common.setPostHold(true);
  }

  private void processTransactionCreated(NetworkCommon common) {
    common.setApprovedAmount(common.getRequestedAmount());

    common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.APPROVED);
    common.setPostAdjustment(true);

    NetworkMessage networkMessage =
        getPriorAuthNetworkMessage(common.getStripeAuthorizationExternalRef());
    common.setNetworkMessageGroupId(networkMessage.getNetworkMessageGroupId());
    Hold hold =
        common.getAccount().getHolds().stream()
            .filter(e -> e.getId().equals(networkMessage.getHoldId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new RecordNotFoundException(Table.NETWORK_MESSAGE, networkMessage.getHoldId()));
    hold.setStatus(HoldStatus.RELEASED);
    common.getUpdatedHolds().add(hold);
    common.getAccount().recalculateAvailableBalance();
    accountActivityService.recordNetworkHoldReleaseAccountActivity(hold);
  }

  private NetworkMessage getPriorAuthNetworkMessage(String externalRef) {
    // this should always return a network message if we received and processed the initial
    // issuing_authorization.request
    return networkMessageRepository
        .findByExternalRefAndTypeOrderByCreatedDesc(externalRef, NetworkMessageType.AUTH_REQUEST)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new RecordNotFoundException(
                    Table.NETWORK_MESSAGE, externalRef, NetworkMessageType.AUTH_REQUEST));
  }
}
