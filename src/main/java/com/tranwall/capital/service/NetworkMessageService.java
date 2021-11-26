package com.tranwall.capital.service;

import com.tranwall.capital.data.model.NetworkMessage;
import com.tranwall.capital.data.repository.NetworkMessageRepository;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.AccountService.HoldRecord;
import com.tranwall.capital.service.CardService.CardRecord;
import com.tranwall.capital.service.type.NetworkCommon;
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

  @Transactional
  public NetworkMessage processNetworkMessage(NetworkCommon common) {
    common.getAmount().ensurePositive();
    CardRecord cardRecord = cardService.getCardByCardNumber(common.getCardNumber());
    common.setBusinessId(cardRecord.card().getBusinessId());
    common.setCard(cardRecord.card());
    common.setAccount(cardRecord.account());
    common.setAllocation(
        allocationService.retrieveAllocation(
            common.getBusinessId(), cardRecord.card().getAllocationId()));

    switch (common.getNetworkMessageType()) {
      case PRE_AUTH_TRANSACTION, PRE_AUTH_TRANSACTION_ADVICE -> processPreAuth(common);
      case FINANCIAL_TRANSACTION, FINANCIAL_TRANSACTION_ADVICE -> processFinancialAuth(common);
      case REVERSAL_TRANSACTION, REVERSAL_TRANSACTION_ADVICE -> processReversal(common);
      case SERVICE_FEE_TRANSACTION -> processServiceFee(common);
      default -> throw new IllegalArgumentException(
          "invalid networkMessageType " + common.getNetworkMessageType());
    }

    NetworkMessage networkMessage =
        new NetworkMessage(
            cardRecord.card().getBusinessId(),
            cardRecord.card().getAllocationId(),
            UUID.randomUUID(),
            common.getNetworkMessageType(),
            common.getAmount(),
            common.getMerchantName(),
            common.getMerchantAddress(),
            common.getMerchantNumber(),
            common.getMerchantCategoryCode());

    if (common.getCard() != null) {
      networkMessage.setCardId(common.getCard().getId());
    }

    if (common.isPostHold()) {
      HoldRecord holdRecord =
          accountService.recordNetworkHold(
              common.getAccount(),
              common.getCreditOrDebit(),
              common.getAmount(),
              OffsetDateTime.now().plusDays(2));
      networkMessage.setHoldId(holdRecord.hold().getId());
      accountActivityService.recordNetworkHoldAccountAccountActivity(common, holdRecord.hold());
    }

    if (common.isPostAdjustment()) {
      AdjustmentRecord adjustmentRecord =
          accountService.recordNetworkAdjustment(
              common.getAccount(), common.getCreditOrDebit(), common.getAmount());
      networkMessage.setAdjustmentId(adjustmentRecord.adjustment().getId());
      accountActivityService.recordNetworkAdjustmentAccountAccountActivity(
          common, adjustmentRecord.adjustment());
    }

    if (common.isPostDecline()) {
      accountActivityService.recordNetworkDeclineAccountAccountActivity(common);
    }

    return networkMessageRepository.save(networkMessage);
  }

  private void processPreAuth(NetworkCommon common) {
    if (common.getAmount().isGreaterThan(common.getAccount().getAvailableBalance())) {
      common.setPostDecline(true);
      return;
    }

    common.setPostHold(true);
  }

  private void processFinancialAuth(NetworkCommon common) {
    if (common.getAmount().isGreaterThan(common.getAccount().getAvailableBalance())) {
      common.setPostDecline(true);
      return;
    }

    common.setPostAdjustment(true);
  }

  private void processReversal(NetworkCommon common) {
    common.setPostAdjustment(true);
  }

  private void processServiceFee(NetworkCommon common) {}
}
