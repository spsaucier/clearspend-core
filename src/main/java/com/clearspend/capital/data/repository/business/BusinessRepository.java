package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.business.Business;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, TypedId<BusinessId>> {

  Optional<Business> findByStripeAccountReference(String stripeAccountReference);

  Optional<Business> findByStripeFinancialAccountRef(String stripeFinancialAccountRef);
}
