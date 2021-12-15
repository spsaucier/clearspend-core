package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessBankAccountBalanceId;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.BusinessBankAccountBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessBankAccountBalanceRepository
    extends JpaRepository<BusinessBankAccountBalance, TypedId<BusinessBankAccountBalanceId>> {

  Optional<BusinessBankAccountBalance> findFirstByBusinessBankAccountIdOrderByCreatedDesc(
      TypedId<BusinessBankAccountId> businessBankAccountId);
}
