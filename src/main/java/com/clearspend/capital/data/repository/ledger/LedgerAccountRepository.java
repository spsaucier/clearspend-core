package com.clearspend.capital.data.repository.ledger;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.ledger.LedgerAccountId;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.model.ledger.LedgerAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository
    extends JpaRepository<LedgerAccount, TypedId<LedgerAccountId>> {

  Optional<LedgerAccount> findByTypeAndCurrency(
      LedgerAccountType ledgerAccountType, Currency currency);
}
