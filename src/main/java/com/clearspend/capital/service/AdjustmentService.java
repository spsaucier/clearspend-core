package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import com.clearspend.capital.data.repository.AdjustmentRepository;
import com.clearspend.capital.service.LedgerService.BankJournalEntry;
import com.clearspend.capital.service.LedgerService.ManualAdjustmentJournalEntry;
import com.clearspend.capital.service.LedgerService.NetworkJournalEntry;
import com.clearspend.capital.service.LedgerService.ReallocationJournalEntry;
import java.time.OffsetDateTime;
import java.util.List;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdjustmentService {

  private final LedgerService ledgerService;

  private final AdjustmentRepository adjustmentRepository;

  public record ReallocateFundsRecord(
      ReallocationJournalEntry bankJournalEntry,
      Adjustment fromAdjustment,
      Adjustment toAdjustment) {}

  public record AdjustmentRecord(JournalEntry journalEntry, Adjustment adjustment) {}

  @Transactional(TxType.REQUIRED)
  Adjustment recordDepositFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordDepositFunds(account.getLedgerAccountId(), amount);

    return adjustmentRepository.save(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.DEPOSIT,
            OffsetDateTime.now(),
            amount));
  }

  @Transactional(TxType.REQUIRED)
  Adjustment recordWithdrawFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordWithdrawFunds(account.getLedgerAccountId(), amount);

    return adjustmentRepository.save(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.WITHDRAW,
            OffsetDateTime.now(),
            amount.negate()));
  }

  @Transactional(TxType.REQUIRED)
  Adjustment recordApplyFee(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordApplyFee(account.getLedgerAccountId(), amount);

    return adjustmentRepository.save(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.FEE,
            OffsetDateTime.now(),
            amount.negate()));
  }

  @Transactional(TxType.REQUIRED)
  ReallocateFundsRecord reallocateFunds(Account fromAccount, Account toAccount, Amount amount) {
    ReallocationJournalEntry bankJournalEntry =
        ledgerService.recordReallocateFunds(
            fromAccount.getLedgerAccountId(), toAccount.getLedgerAccountId(), amount);

    Adjustment fromAdjustment =
        adjustmentRepository.save(
            new Adjustment(
                fromAccount.getBusinessId(),
                fromAccount.getAllocationId(),
                fromAccount.getId(),
                fromAccount.getLedgerAccountId(),
                bankJournalEntry.journalEntry().getId(),
                bankJournalEntry.fromPosting().getId(),
                AdjustmentType.REALLOCATE,
                OffsetDateTime.now(),
                amount.negate()));

    Adjustment toAdjustment =
        adjustmentRepository.save(
            new Adjustment(
                toAccount.getBusinessId(),
                toAccount.getAllocationId(),
                toAccount.getId(),
                toAccount.getLedgerAccountId(),
                bankJournalEntry.journalEntry().getId(),
                bankJournalEntry.toPosting().getId(),
                AdjustmentType.REALLOCATE,
                OffsetDateTime.now(),
                amount));

    return new ReallocateFundsRecord(bankJournalEntry, fromAdjustment, toAdjustment);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentRecord recordManualAdjustment(Account account, Amount amount) {
    ManualAdjustmentJournalEntry manualAdjustmentJournalEntry =
        ledgerService.recordManualAdjustment(account.getLedgerAccountId(), amount);

    Adjustment adjustment =
        adjustmentRepository.save(
            new Adjustment(
                account.getBusinessId(),
                account.getAllocationId(),
                account.getId(),
                account.getLedgerAccountId(),
                manualAdjustmentJournalEntry.journalEntry().getId(),
                manualAdjustmentJournalEntry.accountPosting().getId(),
                AdjustmentType.MANUAL,
                OffsetDateTime.now(),
                manualAdjustmentJournalEntry.accountPosting().getAmount()));

    account.setLedgerBalance(account.getLedgerBalance().add(amount));
    account.recalculateAvailableBalance();

    return new AdjustmentRecord(manualAdjustmentJournalEntry.journalEntry(), adjustment);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentRecord recordNetworkAdjustment(Account account, Amount amount) {
    NetworkJournalEntry networkJournalEntry =
        ledgerService.recordNetworkAdjustment(account.getLedgerAccountId(), amount);

    Adjustment adjustment =
        adjustmentRepository.save(
            new Adjustment(
                account.getBusinessId(),
                account.getAllocationId(),
                account.getId(),
                account.getLedgerAccountId(),
                networkJournalEntry.journalEntry().getId(),
                networkJournalEntry.accountPosting().getId(),
                AdjustmentType.NETWORK,
                OffsetDateTime.now(),
                networkJournalEntry.accountPosting().getAmount()));

    return new AdjustmentRecord(networkJournalEntry.journalEntry(), adjustment);
  }

  List<Adjustment> retrieveBusinessAdjustments(
      TypedId<BusinessId> businessId, List<AdjustmentType> adjustmentTypes, int daysAgo) {
    OffsetDateTime before = OffsetDateTime.now().minusDays(daysAgo);
    return adjustmentRepository.findByBusinessIdAndTypeInAndEffectiveDateAfter(
        businessId, adjustmentTypes, before);
  }
}
