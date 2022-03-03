package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.business.Business;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BusinessRepository extends JpaRepository<Business, TypedId<BusinessId>> {

  @Query("select b from Business b where b.stripeData.accountRef = ?1")
  Optional<Business> findByStripeAccountRef(String stripeAccountReference);

  @Query("select b from Business b where b.stripeData.financialAccountRef = ?1")
  Optional<Business> findByStripeFinancialAccountRef(String stripeFinancialAccountRef);

  Optional<Business> findByEmployerIdentificationNumber(String employerIdentificationNumber);

  Optional<Business> findByCodatCompanyRef(String codatCompanyRef);
}
