package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.LedgerAccount;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  @NonNull private final AccountRepository accountRepository;

  @NonNull private final LedgerService ledgerService;

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
}
