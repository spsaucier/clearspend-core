package com.clearspend.capital.data.repository.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountBalanceId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessBankAccountBalanceRepository
    extends JpaRepository<BusinessBankAccountBalance, TypedId<BusinessBankAccountBalanceId>> {

  @Query(
      """
  DELETE FROM BusinessBankAccountBalance bal
  WHERE bal.businessBankAccountId IN (
    SELECT bank.id
    FROM BusinessBankAccount bank
    WHERE bank.businessId = :businessId
  )
""")
  @Modifying
  void deleteAllByBusinessId(@Param("businessId") final TypedId<BusinessId> businessId);

  Optional<BusinessBankAccountBalance> findFirstByBusinessBankAccountIdOrderByCreatedDesc(
      TypedId<BusinessBankAccountId> businessBankAccountId);
}
