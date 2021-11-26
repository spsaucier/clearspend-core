package com.tranwall.capital.service;

import com.tranwall.capital.client.i2c.I2Client;
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
import com.tranwall.capital.data.model.Allocation;
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
  private final BusinessLimitService businessLimitService;
  private final LedgerService ledgerService;
  private final I2Client i2Client;

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
        new Account(businessId, ledgerAccount.getId(), type, ownerId, Amount.of(currency)));
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentRecord depositFunds(
      TypedId<BusinessId> businessId,
      Account rootAllocationAccount,
      Allocation rootAllocation,
      Amount amount,
      boolean placeHold) {
    amount.ensurePositive();

    businessLimitService.ensureWithinDepositLimit(businessId, amount);

    Adjustment adjustment = adjustmentService.recordDepositFunds(rootAllocationAccount, amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().add(amount));

    if (placeHold) {
      holdRepository.save(
          new Hold(
              businessId,
              rootAllocationAccount.getId(),
              HoldStatus.PLACED,
              amount.negate(),
              OffsetDateTime.now().plusDays(5)));
    }

    // flush everything to the db before trying to call i2c
    holdRepository.flush();

    i2Client.creditFunds(rootAllocation.getI2cAccountRef(), amount.getAmount());

    return new AdjustmentRecord(rootAllocationAccount, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentRecord withdrawFunds(
      TypedId<BusinessId> businessId, Account rootAllocationAccount, Amount amount) {
    amount.ensurePositive();

    if (rootAllocationAccount.getAvailableBalance().isLessThan(amount)) {
      throw new InsufficientFundsException(
          "Account", rootAllocationAccount.getId(), AdjustmentType.WITHDRAW, amount);
    }

    businessLimitService.ensureWithinWithdrawLimit(businessId, amount);

    Adjustment adjustment = adjustmentService.recordWithdrawFunds(rootAllocationAccount, amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().sub(amount));
    rootAllocationAccount = accountRepository.save(rootAllocationAccount);

    return new AdjustmentRecord(rootAllocationAccount, adjustment);
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

  public Account retrieveRootAllocationAccount(
      TypedId<BusinessId> businessId,
      Currency currency,
      TypedId<AllocationId> owner,
      boolean fetchHolds) {
    Account account =
        accountRepository
            .findByBusinessIdAndTypeAndOwnerIdAndLedgerBalance_Currency(
                businessId, AccountType.ALLOCATION, owner.toUuid(), currency)
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
      throw new InsufficientFundsException(
          "Account", fromAccountId, AdjustmentType.REALLOCATE, amount);
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
