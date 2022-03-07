package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.StripeRequirementsId;
import com.clearspend.capital.data.model.business.StripeRequirements;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeRequirementsRepository
    extends JpaRepository<StripeRequirements, TypedId<StripeRequirementsId>> {

  Optional<StripeRequirements> findByBusinessId(TypedId<BusinessId> businessId);
}
