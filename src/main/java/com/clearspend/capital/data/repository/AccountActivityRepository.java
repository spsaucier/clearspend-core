package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.AccountActivity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AccountActivityRepository
    extends JpaRepository<AccountActivity, TypedId<AccountActivityId>>,
        JpaSpecificationExecutor<AccountActivity>,
        AccountActivityRepositoryCustom {

  int countByBusinessId(TypedId<BusinessId> businessId);

  Optional<AccountActivity> findByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<AccountActivityId> id);

  Optional<AccountActivity> findByBusinessIdAndUserIdAndAdjustmentId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<AdjustmentId> adjustmentId);

  @Query(
      value =
          """
      select *
      from account_activity
      where account_activity.businessId = :businessId
        and :receiptId = any(account_activity.receipt_receipt_ids::uuid)
      """,
      nativeQuery = true)
  Optional<AccountActivity> findByBusinessIdAndReceiptId(
      TypedId<BusinessId> businessId, TypedId<ReceiptId> receiptId);

  Optional<AccountActivity> findByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<AccountActivityId> id);

  Optional<AccountActivity> findByHoldId(TypedId<HoldId> holdId);
}
