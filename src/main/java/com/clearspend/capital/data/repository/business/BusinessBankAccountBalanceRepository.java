package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountBalanceId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessBankAccountBalanceRepository
    extends JpaRepository<BusinessBankAccountBalance, TypedId<BusinessBankAccountBalanceId>> {

  Optional<BusinessBankAccountBalance> findFirstByBusinessBankAccountIdOrderByCreatedDesc(
      TypedId<BusinessBankAccountId> businessBankAccountId);
}
