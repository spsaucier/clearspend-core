package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.BusinessLimitId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.BusinessLimit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessLimitRepository
    extends JpaRepository<BusinessLimit, TypedId<BusinessLimitId>> {

  Optional<BusinessLimit> findByBusinessId(TypedId<BusinessId> businessId);

  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
