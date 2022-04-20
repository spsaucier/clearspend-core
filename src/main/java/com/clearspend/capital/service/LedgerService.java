package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InvalidStateException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.ledger.LedgerAccountId;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import com.clearspend.capital.data.model.ledger.LedgerAccount;
import com.clearspend.capital.data.model.ledger.Posting;
import com.clearspend.capital.data.repository.ledger.JournalEntryRepository;
import com.clearspend.capital.data.repository.ledger.LedgerAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

  private final JournalEntryRepository journalEntryRepository;
  private final LedgerAccountRepository ledgerAccountRepository;

  public record BankJournalEntry(
      JournalEntry journalEntry, Posting bankPosting, Posting accountPosting) {}

  public record ManualAdjustmentJournalEntry(
      JournalEntry journalEntry, Posting manualAdjustmentPosting, Posting accountPosting) {}

  public record NetworkJournalEntry(
      JournalEntry journalEntry, Posting networkPosting, Posting accountPosting) {}

  public record ReallocationJournalEntry(
      JournalEntry journalEntry, Posting fromPosting, Posting toPosting) {}

  // TODO(kuchlien): work out a way to do this in the JournalEntry class. I did try adding
  //  @PrePersist but was told that we already had one in Versioned. This is a poor mans solution
  private JournalEntry save(JournalEntry journalEntry) {
    // ensure that the sum of the postings on a journal entry total zero
    BigDecimal sum =
        journalEntry.getPostings().stream()
            .map(e -> e.getAmount().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(BigDecimal.ZERO) != 0) {
      throw new InvalidStateException(
          Table.JOURNAL_ENTRY, "Sum of postings not equal to zero, got " + sum);
    }

    // ensure that each ledgerAccountId in postings is unique
    if (journalEntry.getPostings().size()
        != journalEntry.getPostings().stream()
            .map(Posting::getLedgerAccountId)
            .collect(Collectors.toSet())
            .size()) {
      throw new InvalidStateException(
          Table.JOURNAL_ENTRY, "LedgerAccountId's must be unique in JournalEntry");
    }

    return journalEntryRepository.save(journalEntry);
  }

  // this method will create a new account for an allocation, a business or a card
  LedgerAccount createLedgerAccount(LedgerAccountType type, Currency currency) {
    if (!LedgerAccountType.createOnlySet.contains(type)) {
      throw new IllegalArgumentException("Invalid ledgerAccountType: " + type);
    }

    return ledgerAccountRepository.save(new LedgerAccount(type, currency));
  }

  // this method will create or get an existing system ledgerAccount for non-BUSINESS, ALLOCATION,
  // CARD ledger accounts
  LedgerAccount getOrCreateLedgerAccount(LedgerAccountType type, Currency currency) {
    return ledgerAccountRepository
        .findByTypeAndCurrency(type, currency)
        .orElseGet(() -> ledgerAccountRepository.save(new LedgerAccount(type, currency)));
  }

  @Transactional(TxType.REQUIRED)
  BankJournalEntry recordDepositFunds(TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    return bankFunds(ledgerAccountId, amount);
  }

  @Transactional(TxType.REQUIRED)
  BankJournalEntry recordWithdrawFunds(TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    return bankFunds(ledgerAccountId, amount.negate());
  }

  @Transactional(TxType.REQUIRED)
  BankJournalEntry recordApplyFee(TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    LedgerAccount businessAccount = getLedgerAccount(ledgerAccountId);
    LedgerAccount clearspendAccount =
        getOrCreateLedgerAccount(LedgerAccountType.CLEARSPEND, amount.getCurrency());

    JournalEntry journalEntry = new JournalEntry();
    Posting accountPosting = new Posting(journalEntry, businessAccount.getId(), amount);
    Posting clearspendPosting =
        new Posting(journalEntry, clearspendAccount.getId(), amount.negate());

    journalEntry.setPostings(List.of(clearspendPosting, accountPosting));
    journalEntry = save(journalEntry);

    return new BankJournalEntry(journalEntry, clearspendPosting, accountPosting);
  }

  private BankJournalEntry bankFunds(TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    LedgerAccount businessAccount = getLedgerAccount(ledgerAccountId);
    LedgerAccount bankAccount =
        getOrCreateLedgerAccount(LedgerAccountType.BANK, amount.getCurrency());

    JournalEntry journalEntry = new JournalEntry();
    Posting accountPosting = new Posting(journalEntry, businessAccount.getId(), amount);
    Posting bankPosting = new Posting(journalEntry, bankAccount.getId(), amount.negate());

    journalEntry.setPostings(List.of(bankPosting, accountPosting));
    journalEntry = save(journalEntry);

    return new BankJournalEntry(journalEntry, bankPosting, accountPosting);
  }

  private LedgerAccount getLedgerAccount(TypedId<LedgerAccountId> ledgerAccountId) {
    return ledgerAccountRepository
        .findById(ledgerAccountId)
        .orElseThrow(() -> new RecordNotFoundException(Table.LEDGER_ACCOUNT, ledgerAccountId));
  }

  @Transactional(TxType.REQUIRED)
  ReallocationJournalEntry recordReallocateFunds(
      TypedId<LedgerAccountId> fromLedgerAccountId,
      TypedId<LedgerAccountId> toLedgerAccountId,
      Amount amount) {
    LedgerAccount fromLedgerAccount = getLedgerAccount(fromLedgerAccountId);
    LedgerAccount toLedgerAccount = getLedgerAccount(toLedgerAccountId);

    JournalEntry journalEntry = new JournalEntry();
    Posting fromPosting = new Posting(journalEntry, fromLedgerAccount.getId(), amount.negate());
    Posting toPosting = new Posting(journalEntry, toLedgerAccount.getId(), amount);

    journalEntry.setPostings(List.of(fromPosting, toPosting));
    journalEntry = save(journalEntry);

    return new ReallocationJournalEntry(journalEntry, fromPosting, toPosting);
  }

  @Transactional(TxType.REQUIRED)
  ManualAdjustmentJournalEntry recordManualAdjustment(
      TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    LedgerAccount manualAdjustmentLedgerAccount =
        getOrCreateLedgerAccount(LedgerAccountType.MANUAL, amount.getCurrency());
    LedgerAccount ledgerAccount = getLedgerAccount(ledgerAccountId);

    JournalEntry journalEntry = new JournalEntry();
    Posting manualAdjustmentPosting =
        new Posting(journalEntry, manualAdjustmentLedgerAccount.getId(), amount.negate());
    Posting accountPosting = new Posting(journalEntry, ledgerAccount.getId(), amount);

    journalEntry.setPostings(List.of(manualAdjustmentPosting, accountPosting));
    journalEntry = save(journalEntry);

    return new ManualAdjustmentJournalEntry(journalEntry, manualAdjustmentPosting, accountPosting);
  }

  @Transactional(TxType.REQUIRED)
  NetworkJournalEntry recordNetworkAdjustment(
      TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    LedgerAccount networkLedgerAccount =
        getOrCreateLedgerAccount(LedgerAccountType.NETWORK, amount.getCurrency());
    LedgerAccount ledgerAccount = getLedgerAccount(ledgerAccountId);

    JournalEntry journalEntry = new JournalEntry();
    Posting networkPosting =
        new Posting(journalEntry, networkLedgerAccount.getId(), amount.negate());
    Posting accountPosting = new Posting(journalEntry, ledgerAccount.getId(), amount);

    journalEntry.setPostings(List.of(networkPosting, accountPosting));
    journalEntry = save(journalEntry);

    return new NetworkJournalEntry(journalEntry, networkPosting, accountPosting);
  }
}
