package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.CreditOrDebit;
import com.tranwall.capital.data.repository.AdjustmentRepository;
import com.tranwall.capital.service.LedgerService.BankJournalEntry;
import com.tranwall.capital.service.LedgerService.NetworkJournalEntry;
import com.tranwall.capital.service.LedgerService.ReallocationJournalEntry;
import java.time.OffsetDateTime;
import java.util.List;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.NonNull;
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

  @Transactional(TxType.REQUIRED)
  public Adjustment recordDepositFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordDepositFunds(account.getLedgerAccountId(), amount);

    return adjustmentRepository.save(
        new Adjustment(
            account.getBusinessId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.DEPOSIT,
            OffsetDateTime.now(),
            amount));
  }

  @Transactional(TxType.REQUIRED)
  public Adjustment recordWithdrawFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordWithdrawFunds(account.getLedgerAccountId(), amount);

    return adjustmentRepository.save(
        new Adjustment(
            account.getBusinessId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.WITHDRAW,
            OffsetDateTime.now(),
            amount.negate()));
  }

  @Transactional(TxType.REQUIRED)
  public ReallocateFundsRecord reallocateFunds(
      Account fromAccount, Account toAccount, Amount amount) {
    ReallocationJournalEntry bankJournalEntry =
        ledgerService.recordReallocateFunds(
            fromAccount.getLedgerAccountId(), toAccount.getLedgerAccountId(), amount);

    Adjustment fromAdjustment =
        adjustmentRepository.save(
            new Adjustment(
                fromAccount.getBusinessId(),
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
                fromAccount.getBusinessId(),
                fromAccount.getId(),
                fromAccount.getLedgerAccountId(),
                bankJournalEntry.journalEntry().getId(),
                bankJournalEntry.fromPosting().getId(),
                AdjustmentType.REALLOCATE,
                OffsetDateTime.now(),
                amount));

    return new ReallocateFundsRecord(bankJournalEntry, fromAdjustment, toAdjustment);
  }

  @Transactional(TxType.REQUIRED)
  public Adjustment recordNetworkAdjustment(
      Account account, @NonNull CreditOrDebit creditOrDebit, Amount amount) {
    amount.ensurePositive();

    NetworkJournalEntry networkJournalEntry =
        ledgerService.recordNetworkAdjustment(account.getLedgerAccountId(), creditOrDebit, amount);

    return adjustmentRepository.save(
        new Adjustment(
            account.getBusinessId(),
            account.getId(),
            account.getLedgerAccountId(),
            networkJournalEntry.journalEntry().getId(),
            networkJournalEntry.accountPosting().getId(),
            AdjustmentType.NETWORK,
            OffsetDateTime.now(),
            networkJournalEntry.accountPosting().getAmount()));
  }

  public Adjustment retrieveAdjustment(TypedId<AdjustmentId> id) {
    return adjustmentRepository
        .findById(id)
        .orElseThrow(() -> new RecordNotFoundException(Table.ADJUSTMENT, id));
  }

  public List<Adjustment> retrieveBusinessAdjustments(
      TypedId<BusinessId> businessId, List<AdjustmentType> adjustmentTypes, int daysAgo) {
    OffsetDateTime before = OffsetDateTime.now().minusDays(daysAgo);
    return adjustmentRepository.findByBusinessIdAndTypeInAndEffectiveDateAfter(
        businessId, adjustmentTypes, before);
  }
}
