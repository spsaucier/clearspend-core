package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.Hold;
import com.tranwall.capital.data.model.LedgerAccount;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.CreditOrDebit;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.HoldStatus;
import com.tranwall.capital.data.repository.AccountRepository;
import com.tranwall.capital.data.repository.HoldRepository;
import com.tranwall.capital.service.AdjustmentService.ReallocateFundsRecord;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private final AccountRepository accountRepository;
  private final HoldRepository holdRepository;

  private final AdjustmentService adjustmentService;
  private final LedgerService ledgerService;

  public record AdjustmentRecord(Account account, Adjustment adjustment) {}

  public record HoldRecord(Account account, Hold hold) {}

  public record AccountReallocateFundsRecord(
      Account fromAccount, Account toAccount, ReallocateFundsRecord reallocateFundsRecord) {}

  @Transactional(TxType.REQUIRED)
  public Account createAccount(
      TypedId<BusinessId> businessId, AccountType type, UUID ownerId, Currency currency) {
    LedgerAccount ledgerAccount =
        ledgerService.createLedgerAccount(type.getLedgerAccountType(), currency);

    return accountRepository.save(
        new Account(
            businessId,
            ledgerAccount.getId(),
            type,
            ownerId,
            Amount.of(currency, BigDecimal.ZERO)));
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentRecord depositFunds(
      TypedId<BusinessId> businessId, Amount amount, boolean placeHold) {
    amount.ensurePositive();

    Account account = retrieveBusinessAccount(businessId, amount.getCurrency(), false);

    Adjustment adjustment = adjustmentService.recordDepositFunds(account, amount);
    account.setLedgerBalance(account.getLedgerBalance().add(amount));

    if (placeHold) {
      holdRepository.save(
          new Hold(
              businessId,
              account.getId(),
              HoldStatus.PLACED,
              amount.negate(),
              OffsetDateTime.now().plusDays(5)));
    }

    return new AdjustmentRecord(account, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentRecord withdrawFunds(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensurePositive();

    Account account = retrieveBusinessAccount(businessId, amount.getCurrency(), true);
    if (account.getAvailableBalance().isSmallerThan(amount)) {
      throw new InsufficientFundsException(account.getId(), AdjustmentType.WITHDRAW, amount);
    }

    Adjustment adjustment = adjustmentService.recordWithdrawFunds(account, amount);
    account.setLedgerBalance(account.getLedgerBalance().sub(amount));
    account = accountRepository.save(account);

    return new AdjustmentRecord(account, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public HoldRecord recordNetworkHold(
      Account account,
      @NonNull CreditOrDebit creditOrDebit,
      Amount amount,
      OffsetDateTime expirationDate) {
    amount.ensurePositive();
    Amount holdAmount = creditOrDebit == CreditOrDebit.DEBIT ? amount.negate() : amount;
    Hold hold =
        holdRepository.save(
            new Hold(
                account.getBusinessId(),
                account.getId(),
                HoldStatus.PLACED,
                holdAmount,
                expirationDate));

    account.getHolds().add(hold);
    account.setAvailableBalance(account.getAvailableBalance().add(holdAmount));

    return new HoldRecord(account, hold);
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentRecord recordNetworkAdjustment(
      Account account, @NonNull CreditOrDebit creditOrDebit, @NonNull Amount amount) {
    amount.ensurePositive();

    Adjustment adjustment =
        adjustmentService.recordNetworkAdjustment(account, creditOrDebit, amount);
    account.setLedgerBalance(account.getLedgerBalance().add(adjustment.getAmount()));
    account = accountRepository.save(account);

    return new AdjustmentRecord(account, adjustment);
  }

  private Account retrieveAccount(TypedId<AccountId> accountId, boolean fetchHolds) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT, accountId));

    fetchHolds(account, fetchHolds);

    return account;
  }

  public Account retrieveBusinessAccount(
      TypedId<BusinessId> businessId, Currency currency, boolean fetchHolds) {
    Account account =
        accountRepository
            .findByBusinessIdAndTypeAndOwnerIdAndLedgerBalance_Currency(
                businessId, AccountType.BUSINESS, businessId.toUuid(), currency)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.ACCOUNT, businessId, AccountType.BUSINESS, businessId, currency));

    fetchHolds(account, fetchHolds);

    return account;
  }

  private void fetchHolds(Account account, boolean fetchHolds) {
    if (fetchHolds) {
      account.setHolds(
          holdRepository.findByAccountIdAndStatusAndExpirationDateAfter(
              account.getId(), HoldStatus.PLACED, OffsetDateTime.now()));
    }
  }

  public Account retrieveAllocationAccount(
      TypedId<BusinessId> businessId, Currency currency, TypedId<AllocationId> allocationId) {
    return retrieveAccount(businessId, currency, AccountType.ALLOCATION, allocationId.toUuid());
  }

  public Account retrieveCardAccount(TypedId<AccountId> accountId, boolean fetchHolds) {
    return retrieveAccount(accountId, fetchHolds);
  }

  private Account retrieveAccount(
      TypedId<BusinessId> businessId, Currency currency, AccountType type, UUID ownerId) {
    Account account =
        accountRepository
            .findByBusinessIdAndTypeAndOwnerIdAndLedgerBalance_Currency(
                businessId, type, ownerId, currency)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.ACCOUNT, businessId, type, businessId));

    fetchHolds(account, true);

    return account;
  }

  public List<Account> retrieveAllocationAccounts(
      TypedId<BusinessId> businessId,
      Currency currency,
      List<TypedId<AllocationId>> allocationIds) {
    return accountRepository.findByBusinessIdAndTypeAndOwnerIdIsInAndLedgerBalance_Currency(
        businessId,
        AccountType.ALLOCATION,
        allocationIds.stream().map(TypedId::toUuid).toList(),
        currency);
  }

  // used to fetch a small set of accounts and their available balances/holds. Note this is super
  // inefficient
  public List<Account> findAccountsByIds(Set<TypedId<AccountId>> accountIds) {
    List<Account> accounts = accountRepository.findByIdIn(accountIds);
    accounts.forEach(account -> fetchHolds(account, true));
    return accounts;
  }

  @Transactional(TxType.REQUIRED)
  public AccountReallocateFundsRecord reallocateFunds(
      TypedId<AccountId> fromAccountId, TypedId<AccountId> toAccountId, Amount amount) {
    amount.ensurePositive();

    Account fromAccount = retrieveAccount(fromAccountId, true);
    Account toAccount = retrieveAccount(toAccountId, true);
    if (!fromAccount.getBusinessId().equals(toAccount.getBusinessId())) {
      throw new IdMismatchException(
          IdType.BUSINESS_ID, fromAccount.getBusinessId(), toAccount.getBusinessId());
    }
    if (fromAccount.getAvailableBalance().isSmallerThan(amount)) {
      throw new InsufficientFundsException(fromAccountId, AdjustmentType.REALLOCATE, amount);
    }

    ReallocateFundsRecord reallocateFundsRecord =
        adjustmentService.reallocateFunds(fromAccount, toAccount, amount);

    fromAccount.setLedgerBalance(fromAccount.getLedgerBalance().add(amount.negate()));
    toAccount.setLedgerBalance(toAccount.getLedgerBalance().add(amount));
    fromAccount = accountRepository.save(fromAccount);
    toAccount = accountRepository.save(toAccount);

    return new AccountReallocateFundsRecord(fromAccount, toAccount, reallocateFundsRecord);
  }
}
