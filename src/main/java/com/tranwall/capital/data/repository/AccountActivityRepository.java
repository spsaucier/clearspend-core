package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.AccountActivity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccountActivityRepository
    extends JpaRepository<AccountActivity, TypedId<AccountActivityId>>,
        JpaSpecificationExecutor<AccountActivity>,
        AccountActivityRepositoryCustom {

  int countByBusinessId(TypedId<BusinessId> businessId);

  Optional<AccountActivity> findByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<AccountActivityId> id);

  Optional<AccountActivity> findByBusinessIdAndUserIdAndAdjustmentId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<AdjustmentId> adjustmentId);

  Optional<AccountActivity> findByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<AccountActivityId> id);
}
