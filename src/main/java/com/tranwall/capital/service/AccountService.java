package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.AmountException;
import com.tranwall.capital.common.error.AmountException.AmountType;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.LedgerAccount;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.HoldStatus;
import com.tranwall.capital.data.repository.AccountRepository;
import com.tranwall.capital.data.repository.HoldRepository;
import com.tranwall.capital.service.AdjustmentService.ReallocateFundsRecord;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private final AccountRepository accountRepository;
  private final HoldRepository holdRepository;

  private final LedgerService ledgerService;
  private final AdjustmentService adjustmentService;

  public record AdjustmentRecord(Account account, Adjustment adjustment) {}

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
  public AdjustmentRecord depositFunds(TypedId<BusinessId> businessId, Amount amount) {
    if (!amount.isPositive()) {
      throw new AmountException(AmountType.POSITIVE, amount);
    }

    Account account = retrieveBusinessAccount(businessId, amount.getCurrency());

    Adjustment adjustment = adjustmentService.depositFunds(account, amount);
    account.setLedgerBalance(Amount.add(account.getLedgerBalance(), amount));

    return new AdjustmentRecord(account, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public AdjustmentRecord withdrawFunds(TypedId<BusinessId> businessId, Amount amount) {
    if (!amount.isPositive()) {
      throw new AmountException(AmountType.POSITIVE, amount);
    }

    Account account = retrieveBusinessAccount(businessId, amount.getCurrency());
    if (account.getLedgerBalance().isSmallerThan(amount)) {
      throw new InsufficientFundsException(account.getId(), AdjustmentType.WITHDRAW, amount);
    }

    Adjustment adjustment = adjustmentService.withdrawFunds(account, amount);
    account.setLedgerBalance(Amount.sub(account.getLedgerBalance(), amount));
    account = accountRepository.save(account);

    return new AdjustmentRecord(account, adjustment);
  }

  private Account retrieveAccount(TypedId<AccountId> accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT, accountId));
  }

  public Account retrieveBusinessAccount(TypedId<BusinessId> businessId, Currency currency) {
    return accountRepository
        .findByBusinessIdAndTypeAndOwnerIdAndLedgerBalance_Currency(
            businessId, AccountType.BUSINESS, businessId.toUuid(), currency)
        .orElseThrow(
            () ->
                new RecordNotFoundException(
                    Table.ACCOUNT, businessId, AccountType.BUSINESS, businessId, currency));
  }

  public Account retrieveAllocationAccount(
      TypedId<BusinessId> businessId, Currency currency, TypedId<AllocationId> allocationId) {
    return retrieveAccount(businessId, currency, AccountType.ALLOCATION, allocationId.toUuid());
  }

  public Account retrieveCardAccount(
      TypedId<BusinessId> businessId, Currency currency, TypedId<CardId> cardId) {
    return retrieveAccount(businessId, currency, AccountType.CARD, cardId.toUuid());
  }

  private Account retrieveAccount(
      TypedId<BusinessId> businessId, Currency currency, AccountType type, UUID ownerId) {
    Account account =
        accountRepository
            .findByBusinessIdAndTypeAndOwnerIdAndLedgerBalance_Currency(
                businessId, type, ownerId, currency)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.ACCOUNT, businessId, type, businessId));
    account.setHolds(
        holdRepository.findByAccountIdAndStatusOrExpirationDateAfter(
            account.getId(), HoldStatus.PLACED, OffsetDateTime.now()));

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

  @Transactional
  public AccountReallocateFundsRecord reallocateFunds(
      TypedId<AccountId> fromAccountId, TypedId<AccountId> toAccountId, Amount amount) {
    if (!amount.isPositive()) {
      throw new AmountException(AmountType.POSITIVE, amount);
    }

    Account fromAccount = retrieveAccount(fromAccountId);
    Account toAccount = retrieveAccount(toAccountId);
    if (!fromAccount.getBusinessId().equals(toAccount.getBusinessId())) {
      throw new IdMismatchException(
          IdType.BUSINESS_ID, fromAccount.getBusinessId(), toAccount.getBusinessId());
    }
    if (fromAccount.getLedgerBalance().isSmallerThan(amount)) {
      throw new InsufficientFundsException(fromAccountId, AdjustmentType.REALLOCATE, amount);
    }

    ReallocateFundsRecord reallocateFundsRecord =
        adjustmentService.reallocateFunds(fromAccount, toAccount, amount);

    fromAccount.setLedgerBalance(Amount.add(fromAccount.getLedgerBalance(), Amount.negate(amount)));
    toAccount.setLedgerBalance(Amount.add(toAccount.getLedgerBalance(), amount));
    fromAccount = accountRepository.save(fromAccount);
    toAccount = accountRepository.save(toAccount);

    return new AccountReallocateFundsRecord(fromAccount, toAccount, reallocateFundsRecord);
  }
}
