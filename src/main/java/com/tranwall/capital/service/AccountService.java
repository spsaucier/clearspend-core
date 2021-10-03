package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.AmountException;
import com.tranwall.capital.common.error.AmountException.AmountType;
import com.tranwall.capital.common.error.BusinessIdMismatchException;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.error.InvalidAccountTypeException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.LedgerAccount;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.AccountRepository;
import com.tranwall.capital.service.AdjustmentService.ReallocateFundsRecord;
import java.math.BigDecimal;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private final AccountRepository accountRepository;

  private final LedgerService ledgerService;
  private final AdjustmentService adjustmentService;

  public record AdjustmentRecord(Account account, Adjustment adjustment) {}

  public record AccountReallocateFundsRecord(
      Account fromAccount, Account toAccount, ReallocateFundsRecord reallocateFundsRecord) {}

  @Transactional
  public Account createAccount(UUID businessId, AccountType type, UUID ownerId, Currency currency) {
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

  @Transactional
  public AdjustmentRecord depositFunds(UUID accountId, Amount amount) {
    if (!amount.isPositive()) {
      throw new AmountException(AmountType.POSITIVE, amount);
    }

    Account account = getAccount(accountId);
    if (account.getType() != AccountType.BUSINESS) {
      throw new InvalidAccountTypeException(AccountType.BUSINESS, account.getType());
    }

    Adjustment adjustment = adjustmentService.depositFunds(account, amount);
    account.setLedgerBalance(Amount.add(account.getLedgerBalance(), amount));

    return new AdjustmentRecord(account, adjustment);
  }

  @Transactional
  public AdjustmentRecord withdrawFunds(UUID accountId, Amount amount) {
    if (!amount.isPositive()) {
      throw new AmountException(AmountType.POSITIVE, amount);
    }

    Account account = getAccount(accountId);
    if (account.getType() != AccountType.BUSINESS) {
      throw new InvalidAccountTypeException(AccountType.BUSINESS, account.getType());
    }
    if (account.getLedgerBalance().isSmallerThan(amount)) {
      throw new InsufficientFundsException(accountId, AdjustmentType.WITHDRAW, amount);
    }

    Adjustment adjustment = adjustmentService.withdrawFunds(account, amount);
    account.setLedgerBalance(Amount.sub(account.getLedgerBalance(), amount));
    account = accountRepository.save(account);

    return new AdjustmentRecord(account, adjustment);
  }

  private Account getAccount(UUID accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT, accountId));
  }

  @Transactional
  public AccountReallocateFundsRecord reallocateFunds(
      UUID fromAccountId, UUID toAccountId, Amount amount) {
    if (!amount.isPositive()) {
      throw new AmountException(AmountType.POSITIVE, amount);
    }

    Account fromAccount = getAccount(fromAccountId);
    Account toAccount = getAccount(toAccountId);
    if (!fromAccount.getBusinessId().equals(toAccount.getBusinessId())) {
      throw new BusinessIdMismatchException(fromAccount.getBusinessId(), toAccount.getBusinessId());
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
