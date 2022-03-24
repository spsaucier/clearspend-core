package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.PendingStripeTransferId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.PendingStripeTransfer;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.PendingStripeTransferState;
import com.clearspend.capital.data.repository.PendingStripeTransferRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PendingStripeTransferService {

  private final RetrievalService retrievalService;
  private final StripeClient stripeClient;
  private final PendingStripeTransferRepository pendingStripeTransferRepository;

  public List<PendingStripeTransfer> retrievePendingTransfers(TypedId<BusinessId> businessId) {
    return pendingStripeTransferRepository.findByBusinessIdAndState(
        businessId, PendingStripeTransferState.PENDING);
  }

  @Transactional
  public TypedId<PendingStripeTransferId> createStripeTransfer(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      TypedId<AdjustmentId> adjustmentId,
      TypedId<HoldId> holdId,
      Amount amount,
      String description,
      String statementDescriptor) {
    PendingStripeTransfer pendingStripeTransfer =
        new PendingStripeTransfer(
            BankAccountTransactType.DEPOSIT,
            amount,
            PendingStripeTransferState.PENDING,
            businessId,
            businessBankAccountId,
            adjustmentId);

    pendingStripeTransfer.setHoldId(holdId);
    pendingStripeTransfer.setDescription(description);
    pendingStripeTransfer.setStatementDescriptor(statementDescriptor);

    return pendingStripeTransferRepository.save(pendingStripeTransfer).getId();
  }

  @Transactional
  public void executeStripeTransfer(TypedId<PendingStripeTransferId> transferId) {
    PendingStripeTransfer pendingStripeTransfer = retrieve(transferId);
    Business business =
        retrievalService.retrieveBusiness(pendingStripeTransfer.getBusinessId(), true);
    BusinessBankAccount businessBankAccount =
        retrievalService.retrieveBusinessBankAccount(
            pendingStripeTransfer.getBusinessBankAccountId());
    switch (pendingStripeTransfer.getTransactType()) {
      case DEPOSIT -> stripeClient.executeInboundTransfer(
          pendingStripeTransfer.getBusinessId(),
          businessBankAccount.getId(),
          pendingStripeTransfer.getAdjustmentId(),
          pendingStripeTransfer.getHoldId(),
          business.getStripeData().getAccountRef(),
          businessBankAccount.getStripeBankAccountRef(),
          business.getStripeData().getFinancialAccountRef(),
          pendingStripeTransfer.getAmount(),
          pendingStripeTransfer.getDescription(),
          pendingStripeTransfer.getStatementDescriptor());

      case WITHDRAW -> throw new RuntimeException(
          "Pending stripe transfers do not support withdraws");
    }

    pendingStripeTransfer.setState(PendingStripeTransferState.COMPLETE);
  }

  protected PendingStripeTransfer retrieve(TypedId<PendingStripeTransferId> transferId) {
    return pendingStripeTransferRepository
        .findById(transferId)
        .orElseThrow(() -> new RecordNotFoundException(Table.PENDING_STRIPE_TRANSFER, transferId));
  }

  // @Async
  public void executePendingStripeTransfers(TypedId<BusinessId> businessId) {
    retrievePendingTransfers(businessId)
        .forEach(transfer -> executeStripeTransfer(transfer.getId()));
  }
}
