package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.LedgerAccount;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LedgerAccountType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
  Optional<LedgerAccount> findByTypeAndCurrency(
      LedgerAccountType ledgerAccountType, Currency currency);
}
