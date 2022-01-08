package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessLimitId;
import com.clearspend.capital.data.model.business.BusinessLimit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessLimitRepository
    extends JpaRepository<BusinessLimit, TypedId<BusinessLimitId>> {

  Optional<BusinessLimit> findByBusinessId(TypedId<BusinessId> businessId);

  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
