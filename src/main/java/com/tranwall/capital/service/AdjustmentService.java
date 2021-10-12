package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.repository.AdjustmentRepository;
import com.tranwall.capital.service.LedgerService.BankJournalEntry;
import com.tranwall.capital.service.LedgerService.ReallocationJournalEntry;
import java.time.OffsetDateTime;
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
      Adjustment toAdjustment) {

  }

  @Transactional(TxType.REQUIRED)
  public Adjustment depositFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.depositFunds(account.getLedgerAccountId(), amount);

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
  public Adjustment withdrawFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.withdrawFunds(account.getLedgerAccountId(), amount);

    return adjustmentRepository.save(
        new Adjustment(
            account.getBusinessId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.WITHDRAW,
            OffsetDateTime.now(),
            Amount.negate(amount)));
  }

  @Transactional(TxType.REQUIRED)
  public ReallocateFundsRecord reallocateFunds(
      Account fromAccount, Account toAccount, Amount amount) {
    ReallocationJournalEntry bankJournalEntry =
        ledgerService.reallocateFunds(
            fromAccount.getLedgerAccountId(), toAccount.getLedgerAccountId(), amount);

    Adjustment fromAdjustment =
        new Adjustment(
            fromAccount.getBusinessId(),
            fromAccount.getId(),
            fromAccount.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.fromPosting().getId(),
            AdjustmentType.REALLOCATE,
            OffsetDateTime.now(),
            Amount.negate(amount));

    Adjustment toAdjustment =
        new Adjustment(
            fromAccount.getBusinessId(),
            fromAccount.getId(),
            fromAccount.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.fromPosting().getId(),
            AdjustmentType.REALLOCATE,
            OffsetDateTime.now(),
            amount);

    return new ReallocateFundsRecord(bankJournalEntry, fromAdjustment, toAdjustment);
  }
}
