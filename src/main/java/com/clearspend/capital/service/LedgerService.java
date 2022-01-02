package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.common.typedid.data.LedgerAccountId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.JournalEntry;
import com.clearspend.capital.data.model.LedgerAccount;
import com.clearspend.capital.data.model.Posting;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.repository.JournalEntryRepository;
import com.clearspend.capital.data.repository.LedgerAccountRepository;
import java.util.List;
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

  public record NetworkJournalEntry(
      JournalEntry journalEntry, Posting networkPosting, Posting accountPosting) {}

  public record ReallocationJournalEntry(
      JournalEntry journalEntry, Posting fromPosting, Posting toPosting) {}

  // this method will create a new account for an allocation, a business or a card
  public LedgerAccount createLedgerAccount(LedgerAccountType type, Currency currency) {
    if (!LedgerAccountType.createOnlySet.contains(type)) {
      throw new IllegalArgumentException("Invalid ledgerAccountType: " + type);
    }

    return ledgerAccountRepository.save(new LedgerAccount(type, currency));
  }

  // this method will create or get an existing system ledgerAccount for non-BUSINESS, ALLOCATION,
  // CARD ledger accounts
  public LedgerAccount getOrCreateLedgerAccount(LedgerAccountType type, Currency currency) {
    return ledgerAccountRepository
        .findByTypeAndCurrency(type, currency)
        .orElseGet(() -> ledgerAccountRepository.save(new LedgerAccount(type, currency)));
  }

  @Transactional(TxType.REQUIRED)
  public BankJournalEntry recordDepositFunds(
      TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    return bankFunds(ledgerAccountId, amount);
  }

  @Transactional(TxType.REQUIRED)
  public BankJournalEntry recordWithdrawFunds(
      TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    return bankFunds(ledgerAccountId, amount.negate());
  }

  private BankJournalEntry bankFunds(TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    LedgerAccount businessAccount = getLedgerAccount(ledgerAccountId);
    LedgerAccount bankAccount =
        getOrCreateLedgerAccount(LedgerAccountType.BANK, amount.getCurrency());

    JournalEntry journalEntry = new JournalEntry();
    Posting accountPosting = new Posting(journalEntry, businessAccount.getId(), amount);
    Posting bankPosting = new Posting(journalEntry, bankAccount.getId(), amount.negate());

    journalEntry.setPostings(List.of(bankPosting, accountPosting));
    journalEntry = journalEntryRepository.save(journalEntry);

    return new BankJournalEntry(journalEntry, bankPosting, accountPosting);
  }

  private LedgerAccount getLedgerAccount(TypedId<LedgerAccountId> ledgerAccountId) {
    return ledgerAccountRepository
        .findById(ledgerAccountId)
        .orElseThrow(() -> new RecordNotFoundException(Table.LEDGER_ACCOUNT, ledgerAccountId));
  }

  @Transactional(TxType.REQUIRED)
  public ReallocationJournalEntry recordReallocateFunds(
      TypedId<LedgerAccountId> fromLedgerAccountId,
      TypedId<LedgerAccountId> toLedgerAccountId,
      Amount amount) {
    LedgerAccount fromLedgerAccount = getLedgerAccount(fromLedgerAccountId);
    LedgerAccount toLedgerAccount = getLedgerAccount(toLedgerAccountId);

    JournalEntry journalEntry = new JournalEntry();
    Posting fromPosting = new Posting(journalEntry, fromLedgerAccount.getId(), amount.negate());
    Posting toPosting = new Posting(journalEntry, toLedgerAccount.getId(), amount);

    journalEntry.setPostings(List.of(fromPosting, toPosting));
    journalEntryRepository.save(journalEntry);

    return new ReallocationJournalEntry(journalEntry, fromPosting, toPosting);
  }

  @Transactional(TxType.REQUIRED)
  public NetworkJournalEntry recordNetworkAdjustment(
      TypedId<LedgerAccountId> ledgerAccountId, Amount amount) {
    LedgerAccount networkLedgerAccount =
        getOrCreateLedgerAccount(LedgerAccountType.NETWORK, amount.getCurrency());
    LedgerAccount ledgerAccount = getLedgerAccount(ledgerAccountId);

    JournalEntry journalEntry = new JournalEntry();
    Posting networkPosting, accountPosting;
    if (amount.isNegative()) {
      networkPosting = new Posting(journalEntry, networkLedgerAccount.getId(), amount.negate());
      accountPosting = new Posting(journalEntry, ledgerAccount.getId(), amount);
    } else {
      accountPosting = new Posting(journalEntry, ledgerAccount.getId(), amount.negate());
      networkPosting = new Posting(journalEntry, networkLedgerAccount.getId(), amount);
    }

    journalEntry.setPostings(List.of(networkPosting, accountPosting));
    journalEntryRepository.save(journalEntry);

    return new NetworkJournalEntry(journalEntry, networkPosting, accountPosting);
  }
}
