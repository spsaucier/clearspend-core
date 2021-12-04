package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TransactionLimitId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.TransactionLimit;
import com.tranwall.capital.data.model.enums.TransactionLimitType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionLimitRepository
    extends JpaRepository<TransactionLimit, TypedId<TransactionLimitId>> {

  Optional<TransactionLimit> findByBusinessIdAndTypeAndOwnerId(
      TypedId<BusinessId> businessId, TransactionLimitType type, UUID ownerId);

  List<TransactionLimit> findByBusinessIdAndTypeAndOwnerIdIn(
      TypedId<BusinessId> businessId, TransactionLimitType type, List<UUID> ownerId);

  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
