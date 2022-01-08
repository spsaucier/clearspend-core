package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TransactionLimitId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionLimitRepository
    extends JpaRepository<TransactionLimit, TypedId<TransactionLimitId>> {

  Optional<TransactionLimit> findByBusinessIdAndTypeAndOwnerId(
      TypedId<BusinessId> businessId, TransactionLimitType type, UUID ownerId);

  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
