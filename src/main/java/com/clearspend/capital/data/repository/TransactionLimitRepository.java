package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TransactionLimitId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface TransactionLimitRepository
    extends JpaRepository<TransactionLimit, TypedId<TransactionLimitId>> {

  Optional<TransactionLimit> findByBusinessIdAndTypeAndOwnerId(
      TypedId<BusinessId> businessId, TransactionLimitType type, UUID ownerId);

  void deleteByBusinessId(TypedId<BusinessId> businessId);

  @Modifying
  @Transactional
  void deleteByBusinessIdAndTypeAndOwnerId(
      final TypedId<BusinessId> businessId, final TransactionLimitType type, final UUID ownerId);
}
