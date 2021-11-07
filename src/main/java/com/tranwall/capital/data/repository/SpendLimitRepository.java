package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.SpendLimitId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.SpendLimit;
import com.tranwall.capital.data.model.enums.SpendLimitType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpendLimitRepository extends JpaRepository<SpendLimit, TypedId<SpendLimitId>> {

  Optional<SpendLimit> findByBusinessIdAndTypeAndOwnerId(
      TypedId<BusinessId> businessId, SpendLimitType type, UUID ownerId);
}
