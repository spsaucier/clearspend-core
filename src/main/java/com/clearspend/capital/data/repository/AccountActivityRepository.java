package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.AccountActivity;
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

  Optional<AccountActivity> findByBusinessIdAndAdjustmentId(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId);

  Optional<AccountActivity> findByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<AccountActivityId> id);

  Optional<AccountActivity> findByHoldId(TypedId<HoldId> holdId);
}
