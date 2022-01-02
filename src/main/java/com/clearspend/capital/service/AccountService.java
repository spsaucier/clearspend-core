package com.clearspend.capital.service;

import com.clearspend.capital.client.i2c.I2Client;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Decline;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.LedgerAccount;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.DeclineRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.service.AdjustmentService.ReallocateFundsRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
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
  private final DeclineRepository declineRepository;
  private final HoldRepository holdRepository;

  private final AdjustmentService adjustmentService;
  private final BusinessLimitService businessLimitService;
  private final LedgerService ledgerService;
  private final I2Client i2Client;

  public record AdjustmentRecord(Account account, Adjustment adjustment) {}

  public record AdjustmentAndHoldRecord(Account account, Adjustment adjustment, Hold hold) {}

  public record HoldRecord(Account account, Hold hold) {}

  public record AccountReallocateFundsRecord(
      Account fromAccount, Account toAccount, ReallocateFundsRecord reallocateFundsRecord) {}

  @Transactional(TxType.REQUIRED)
  public Account createAccount(
      TypedId<BusinessId> businessId,
      AccountType type,
      TypedId<AllocationId> allocationId,
      TypedId<CardId> cardId,
      Currency currency) {
    LedgerAccount ledgerAccount =
        ledgerService.createLedgerAccount(type.getLedgerAccountType(), currency);

    Account account =
        new Account(businessId, allocationId, ledgerAccount.getId(), type, Amount.of(currency));
    account.setCardId(cardId);

    return accountRepository.save(account);
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentAndHoldRecord depositFunds(
      TypedId<BusinessId> businessId,
      Account rootAllocationAccount,
      Allocation rootAllocation,
      Amount amount,
      boolean placeHold) {
    amount.ensureNonNegative();

    businessLimitService.ensureWithinDepositLimit(businessId, amount);

    Adjustment adjustment = adjustmentService.recordDepositFunds(rootAllocationAccount, amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().add(amount));

    Hold hold = null;
    if (placeHold) {
      hold =
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

    return new AdjustmentAndHoldRecord(rootAllocationAccount, adjustment, hold);
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentAndHoldRecord withdrawFunds(
      TypedId<BusinessId> businessId, Account rootAllocationAccount, Amount amount) {
    amount.ensureNonNegative();

    if (rootAllocationAccount.getAvailableBalance().isLessThan(amount)) {
      throw new InsufficientFundsException(
          "Account", rootAllocationAccount.getId(), AdjustmentType.WITHDRAW, amount);
    }

    businessLimitService.ensureWithinWithdrawLimit(businessId, amount);

    Adjustment adjustment = adjustmentService.recordWithdrawFunds(rootAllocationAccount, amount);
    rootAllocationAccount.setLedgerBalance(rootAllocationAccount.getLedgerBalance().sub(amount));
    rootAllocationAccount = accountRepository.save(rootAllocationAccount);

    return new AdjustmentAndHoldRecord(rootAllocationAccount, adjustment, null);
  }

  @Transactional(TxType.REQUIRED)
  public HoldRecord recordNetworkHold(
      Account account, Amount amount, OffsetDateTime expirationDate) {
    amount.ensureNegative();
    Hold hold =
        holdRepository.save(
            new Hold(
                account.getBusinessId(),
                account.getId(),
                HoldStatus.PLACED,
                amount,
                expirationDate));

    account.getHolds().add(hold);
    account.setAvailableBalance(account.getAvailableBalance().add(amount));

    return new HoldRecord(account, hold);
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentRecord recordNetworkAdjustment(Account account, @NonNull Amount amount) {
    Adjustment adjustment = adjustmentService.recordNetworkAdjustment(account, amount);
    account.setLedgerBalance(account.getLedgerBalance().add(adjustment.getAmount()));
    account = accountRepository.save(account);

    return new AdjustmentRecord(account, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public Decline recordNetworkDecline(
      Account account, Card card, Amount amount, List<DeclineReason> declineReasons) {
    return declineRepository.save(
        new Decline(
            account.getBusinessId(), account.getId(), card.getId(), amount, declineReasons));
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
              account.getId(), HoldStatus.PLACED, OffsetDateTime.now()));
    }
  }

  public Account retrieveAllocationAccount(
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

  public Account retrieveCardAccount(TypedId<AccountId> accountId, boolean fetchHolds) {
    return retrieveAccount(accountId, fetchHolds);
  }

  public List<Account> retrieveAllocationAccounts(
      TypedId<BusinessId> businessId,
      Currency currency,
      List<TypedId<AllocationId>> allocationIds) {
    return accountRepository.findByBusinessIdAndTypeAndAllocationIdIsInAndLedgerBalance_Currency(
        businessId, AccountType.ALLOCATION, allocationIds, currency);
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
