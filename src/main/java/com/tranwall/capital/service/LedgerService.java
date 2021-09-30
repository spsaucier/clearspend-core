package com.tranwall.capital.service;

import com.tranwall.capital.data.model.LedgerAccount;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LedgerAccountType;
import com.tranwall.capital.data.repository.LedgerAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

  private final LedgerAccountRepository ledgerAccountRepository;

  public LedgerAccount createLedgerAccount(LedgerAccountType type, Currency currency) {
    return ledgerAccountRepository.save(new LedgerAccount(type, currency));
  }
}
