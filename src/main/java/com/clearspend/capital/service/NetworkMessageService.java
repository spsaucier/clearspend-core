package com.clearspend.capital.service;

import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
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
import java.util.UUID;
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
  public NetworkMessage processNetworkMessage(NetworkCommon common) {
    // update common with data we have locally
    common.getRequestedAmount().ensurePositive();
    CardRecord cardRecord = cardService.getCardByCardRef(common.getCardRef());
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
      case PRE_AUTH, PRE_AUTH_ADVICE -> processPreAuth(common);
      case FINANCIAL_AUTH, FINANCIAL_AUTH_ADVICE -> processFinancialAuth(common);
      case REVERSAL, REVERSAL_ADVICE -> processReversal(common);
      default -> throw new IllegalArgumentException(
          "invalid networkMessageType " + common.getNetworkMessageType());
    }

    // store any data that resulted from processing the network message from Stripe
    NetworkMessage networkMessage =
        new NetworkMessage(
            cardRecord.card().getBusinessId(),
            cardRecord.card().getAllocationId(),
            UUID.randomUUID(),
            common.getNetworkMessageType(),
            common.getRequestedAmount(),
            common.getMerchantName(),
            common.getMerchantAddress(),
            common.getMerchantNumber(),
            common.getMerchantCategoryCode());

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
              common.getAccount(),
              common.getCreditOrDebit(),
              common.getRequestedAmount(),
              OffsetDateTime.now().plusDays(2));
      networkMessage.setHoldId(holdRecord.hold().getId());
      common.getAccountActivity().setActivityTime(holdRecord.hold().getCreated());
      common.getAccountActivity().setHideAfter(holdRecord.hold().getExpirationDate());
      accountActivityService.recordNetworkHoldAccountAccountActivity(common, holdRecord.hold());
      log.debug(
          "networkMessage {} hold {} (available {} / ledger {})",
          networkMessage.getId(),
          networkMessage.getHoldId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    if (common.isPostAdjustment()) {
      AdjustmentRecord adjustmentRecord =
          accountService.recordNetworkAdjustment(
              common.getAccount(), common.getCreditOrDebit(), common.getRequestedAmount());
      networkMessage.setAdjustmentId(adjustmentRecord.adjustment().getId());
      common.getAccountActivity().setActivityTime(adjustmentRecord.adjustment().getCreated());
      accountActivityService.recordNetworkAdjustmentAccountAccountActivity(
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
      common.getAccountActivity().setAccountActivityStatus(AccountActivityStatus.DECLINED);
      common.getAccountActivity().setActivityTime(OffsetDateTime.now());
      accountActivityService.recordNetworkDeclineAccountAccountActivity(common);
      log.debug(
          "networkMessage {} declined (available {} / ledger {})",
          networkMessage.getId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }

    return networkMessageRepository.save(networkMessage);
  }

  private void processPreAuth(NetworkCommon common) {
    if (common.getRequestedAmount().isGreaterThan(common.getAccount().getAvailableBalance())) {
      common.setPostDecline(true);
      return;
    }

    common.getAccountActivity().setAccountActivityStatus(AccountActivityStatus.PENDING);
    common.setPostHold(true);
  }

  private void processFinancialAuth(NetworkCommon common) {
    if (common.getRequestedAmount().isGreaterThan(common.getAccount().getAvailableBalance())) {
      common.setPostDecline(true);
      return;
    }

    common.getAccountActivity().setAccountActivityStatus(AccountActivityStatus.APPROVED);
    common.setPostAdjustment(true);
  }

  private void processReversal(NetworkCommon common) {
    common.setPostAdjustment(true);
  }

  private void processServiceFee(NetworkCommon common) {}
}
