package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.decline.Decline;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import com.clearspend.capital.data.model.ledger.LedgerAccount;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.DeclineRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.service.AdjustmentService.ReallocateFundsRecord;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private final AccountRepository accountRepository;
  private final DeclineRepository declineRepository;
  private final HoldRepository holdRepository;

  private final AdjustmentService adjustmentService;
  private final BusinessSettingsService businessSettingsService;
  private final LedgerService ledgerService;
  private final ApplicationEventPublisher eventPublisher;

  public record AdjustmentRecord(
      Account account, Adjustment adjustment, JournalEntry journalEntry) {}

  public record AdjustmentAndHoldRecord(Account account, Adjustment adjustment, Hold hold) {}

  public record HoldRecord(Account account, Hold hold) {}

  public record HoldCreatedEvent(Hold hold) {}

  public record AccountReallocateFundsRecord(
      Account fromAccount, Account toAccount, ReallocateFundsRecord reallocateFundsRecord) {}

  @Transactional(TxType.REQUIRED)
  Account createAccount(
      TypedId<BusinessId> businessId,
      AccountType type,
      TypedId<AllocationId> allocationId,
      TypedId<CardId> cardId,
      Currency currency) {
    LedgerAccount ledgerAccount =
        ledgerService.createLedgerAccount(type.getLedgerAccountType(), currency);

    Account account = new Account(businessId, ledgerAccount.getId(), type, Amount.of(currency));
    account.setAllocationId(allocationId);
    account.setCardId(cardId);

    return accountRepository.save(account);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentRecord manualAdjustment(Account account, Amount amount) {
    if (amount.isNegative() && account.getAvailableBalance().add(amount).isLessThanZero()) {
      throw new InsufficientFundsException(account, AdjustmentType.MANUAL, amount);
    }

    AdjustmentService.AdjustmentRecord adjustmentRecord =
        adjustmentService.recordManualAdjustment(account, amount);

    return new AdjustmentRecord(
        account, adjustmentRecord.adjustment(), adjustmentRecord.journalEntry());
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentAndHoldRecord depositFunds(
      TypedId<BusinessId> businessId,
      Account rootAllocationAccount,
      Amount amount,
      boolean standardHold) {
    amount.ensureNonNegative();

    businessSettingsService.ensureWithinDepositLimit(businessId, amount);

    OffsetDateTime holdExpirationDate =
        standardHold
            ? OffsetDateTime.now(ZoneOffset.UTC).plusDays(5)
            : OffsetDateTime.now(ZoneOffset.UTC);

    Adjustment adjustment =
        adjustmentService.recordDepositFunds(rootAllocationAccount, holdExpirationDate, amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().add(amount));

    Hold hold =
        saveAndPublishHold(
            new Hold(
                businessId,
                rootAllocationAccount.getId(),
                HoldStatus.PLACED,
                amount.negate(),
                holdExpirationDate));
    log.debug("creating ACH hold {} for account {}", hold.getId(), rootAllocationAccount.getId());

    return new AdjustmentAndHoldRecord(rootAllocationAccount, adjustment, hold);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentAndHoldRecord depositExternalAchFunds(Account rootAllocationAccount, Amount amount) {
    amount.ensureNonNegative();

    Adjustment adjustment =
        adjustmentService.recordDepositFunds(
            rootAllocationAccount, OffsetDateTime.now(Clock.systemUTC()), amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().add(amount));

    return new AdjustmentAndHoldRecord(rootAllocationAccount, adjustment, null);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentAndHoldRecord returnFunds(Account rootAllocationAccount, Amount amount) {
    Adjustment adjustment = adjustmentService.recordReturnFunds(rootAllocationAccount, amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().add(amount));

    return new AdjustmentAndHoldRecord(rootAllocationAccount, adjustment, null);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentAndHoldRecord returnCardFunds(Account cardAccount, Amount amount) {
    amount.ensureNonNegative();

    Adjustment adjustment = adjustmentService.recordCardReturnFunds(cardAccount, amount);
    cardAccount.setLedgerBalance(cardAccount.getLedgerBalance().add(amount));

    return new AdjustmentAndHoldRecord(cardAccount, adjustment, null);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentAndHoldRecord withdrawFunds(
      TypedId<BusinessId> businessId, Account rootAllocationAccount, Amount amount) {
    amount.ensureNonNegative();

    if (rootAllocationAccount.getAvailableBalance().isLessThan(amount)) {
      throw new InsufficientFundsException(rootAllocationAccount, AdjustmentType.WITHDRAW, amount);
    }

    businessSettingsService.ensureWithinWithdrawLimit(businessId, amount);

    Adjustment adjustment = adjustmentService.recordWithdrawFunds(rootAllocationAccount, amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().sub(amount));
    rootAllocationAccount = accountRepository.save(rootAllocationAccount);

    return new AdjustmentAndHoldRecord(rootAllocationAccount, adjustment, null);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentAndHoldRecord applyFee(TypedId<AccountId> accountId, Amount amount) {
    amount.ensureNonNegative();

    Account account = retrieveAccount(accountId, false);
    Adjustment adjustment = adjustmentService.recordApplyFee(account, amount);
    account.setLedgerBalance(account.getLedgerBalance().sub(amount));
    account = accountRepository.save(account);

    return new AdjustmentAndHoldRecord(account, adjustment, null);
  }

  @Transactional(TxType.REQUIRED)
  HoldRecord recordNetworkHold(Account account, Amount amount, OffsetDateTime expirationDate) {
    amount.ensureNegative();
    Hold hold =
        saveAndPublishHold(
            new Hold(
                account.getBusinessId(),
                account.getId(),
                HoldStatus.PLACED,
                amount,
                expirationDate));

    account.getHolds().add(hold);
    account.recalculateAvailableBalance();

    return new HoldRecord(account, hold);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentRecord recordNetworkAdjustment(
      @NonNull final TypedId<AllocationId> allocationId,
      @NonNull final Account account,
      @NonNull final Amount amount) {
    final AdjustmentService.AdjustmentRecord adjustmentRecord =
        adjustmentService.recordNetworkAdjustment(allocationId, account, amount);
    account.setLedgerBalance(
        account.getLedgerBalance().add(adjustmentRecord.adjustment().getAmount()));
    final Account savedAccount = accountRepository.save(account);
    savedAccount.recalculateAvailableBalance();

    return new AdjustmentRecord(
        savedAccount, adjustmentRecord.adjustment(), adjustmentRecord.journalEntry());
  }

  @SneakyThrows
  @Transactional(TxType.REQUIRED)
  Decline recordNetworkDecline(
      final TypedId<BusinessId> businessId,
      final TypedId<CardId> cardId,
      final TypedId<AccountId> accountId,
      final Amount amount,
      List<DeclineDetails> declineDetails) {
    return declineRepository.save(
        new Decline(businessId, accountId, cardId, amount, declineDetails));
  }

  private Account retrieveAccount(TypedId<AccountId> accountId, boolean fetchHolds) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT, accountId));

    fetchHolds(account, fetchHolds);

    return account;
  }

  Account retrieveRootAllocationAccount(
      TypedId<BusinessId> businessId,
      Currency currency,
      TypedId<AllocationId> allocationId,
      boolean fetchHolds) {
    Account account =
        accountRepository
            .findByBusinessIdAndTypeAndAllocationIdAndLedgerBalance_Currency(
                businessId, AccountType.ALLOCATION, allocationId, currency)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.ACCOUNT, businessId, AccountType.ALLOCATION, businessId, currency));

    fetchHolds(account, fetchHolds);

    return account;
  }

  private void fetchHolds(Account account, boolean fetchHolds) {
    if (fetchHolds) {
      account.setHolds(
          holdRepository.findByAccountIdAndStatusAndExpirationDateAfter(
              account.getId(), HoldStatus.PLACED, OffsetDateTime.now(ZoneOffset.UTC)));
    }
  }

  Account retrieveAllocationAccount(
      TypedId<BusinessId> businessId, Currency currency, TypedId<AllocationId> allocationId) {
    Account account =
        accountRepository
            .findByBusinessIdAndTypeAndAllocationIdAndLedgerBalance_Currency(
                businessId, AccountType.ALLOCATION, allocationId, currency)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.ACCOUNT, businessId, AccountType.ALLOCATION, allocationId, currency));

    fetchHolds(account, true);

    return account;
  }

  Account retrieveAccountById(TypedId<AccountId> accountId, boolean fetchHolds) {
    return retrieveAccount(accountId, fetchHolds);
  }

  List<Account> retrieveAllocationAccounts(
      TypedId<BusinessId> businessId,
      Currency currency,
      List<TypedId<AllocationId>> allocationIds,
      boolean fetchHolds) {
    List<Account> accounts =
        accountRepository.findByBusinessIdAndTypeAndAllocationIdIsInAndLedgerBalance_Currency(
            businessId, AccountType.ALLOCATION, allocationIds, currency);

    if (fetchHolds) {
      Map<TypedId<AccountId>, List<Hold>> holds =
          holdRepository
              .findByAccountIdInAndStatusAndExpirationDateAfter(
                  accounts.stream().map(Account::getId).collect(Collectors.toList()),
                  HoldStatus.PLACED,
                  OffsetDateTime.now(ZoneOffset.UTC))
              .stream()
              .collect(Collectors.groupingBy(Hold::getAccountId));
      accounts.forEach(
          account ->
              account.setHolds(holds.getOrDefault(account.getId(), Collections.emptyList())));
    }

    return accounts;
  }

  List<Account> retrieveBusinessAccounts(TypedId<BusinessId> businessId, boolean fetchHolds) {
    List<Account> accounts = accountRepository.findByBusinessId(businessId);

    if (fetchHolds) {
      Map<TypedId<AccountId>, List<Hold>> holds =
          holdRepository
              .findByBusinessIdAndStatusAndExpirationDateAfter(
                  businessId, HoldStatus.PLACED, OffsetDateTime.now(ZoneOffset.UTC))
              .stream()
              .collect(Collectors.groupingBy(Hold::getAccountId));
      accounts.forEach(
          account ->
              account.setHolds(holds.getOrDefault(account.getId(), Collections.emptyList())));
    }

    return accounts;
  }

  @Transactional(TxType.REQUIRED)
  AccountReallocateFundsRecord reallocateFunds(
      TypedId<AccountId> fromAccountId, TypedId<AccountId> toAccountId, Amount amount) {
    amount.ensureNonNegative();
    if (fromAccountId.equals(toAccountId)) {
      throw new IllegalArgumentException(
          String.format("fromAccountId equals toAccountId: %s", fromAccountId));
    }

    Account fromAccount = retrieveAccount(fromAccountId, true);
    Account toAccount = retrieveAccount(toAccountId, true);
    if (!fromAccount.getBusinessId().equals(toAccount.getBusinessId())) {
      throw new IdMismatchException(
          IdType.BUSINESS_ID, fromAccount.getBusinessId(), toAccount.getBusinessId());
    }
    if (fromAccount.getAvailableBalance().isLessThan(amount)) {
      throw new InsufficientFundsException(fromAccount, AdjustmentType.REALLOCATE, amount);
    }

    ReallocateFundsRecord reallocateFundsRecord =
        adjustmentService.reallocateFunds(fromAccount, toAccount, amount);

    fromAccount.setLedgerBalance(fromAccount.getLedgerBalance().add(amount.negate()));
    toAccount.setLedgerBalance(toAccount.getLedgerBalance().add(amount));
    fromAccount = accountRepository.save(fromAccount);
    toAccount = accountRepository.save(toAccount);

    return new AccountReallocateFundsRecord(fromAccount, toAccount, reallocateFundsRecord);
  }

  Hold retrieveHold(TypedId<BusinessId> businessId, TypedId<HoldId> holdId) {
    return holdRepository
        .findByBusinessIdAndId(businessId, holdId)
        .orElseThrow(() -> new RecordNotFoundException(Table.HOLD, businessId, holdId));
  }

  Hold saveAndPublishHold(final Hold hold) {
    final Hold savedHold = holdRepository.saveAndFlush(hold);
    eventPublisher.publishEvent(new HoldCreatedEvent(savedHold));
    return savedHold;
  }
}
