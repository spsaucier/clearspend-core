package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ChartOfAccountsId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ChartOfAccounts;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountsRepository
    extends JpaRepository<ChartOfAccounts, TypedId<ChartOfAccountsId>> {
  Optional<ChartOfAccounts> findByBusinessId(TypedId<BusinessId> businessId);
}
