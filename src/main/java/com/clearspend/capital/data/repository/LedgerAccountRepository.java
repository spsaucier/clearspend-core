package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.LedgerAccountId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.LedgerAccount;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository
    extends JpaRepository<LedgerAccount, TypedId<LedgerAccountId>> {

  Optional<LedgerAccount> findByTypeAndCurrency(
      LedgerAccountType ledgerAccountType, Currency currency);
}
