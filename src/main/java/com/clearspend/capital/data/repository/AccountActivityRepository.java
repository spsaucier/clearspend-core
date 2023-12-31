package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountActivityRepository
    extends JpaRepository<AccountActivity, TypedId<AccountActivityId>>,
        JpaSpecificationExecutor<AccountActivity>,
        AccountActivityRepositoryCustom {

  int countByBusinessId(TypedId<BusinessId> businessId);

  Optional<AccountActivity> findByBusinessIdAndAdjustmentId(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId);

  @Query(
      value =
          """
      select *
      from account_activity
      where :receiptId = any (account_activity.receipt_receipt_ids)
      """,
      nativeQuery = true)
  // note that we're using UUID here explicitly as I get an error that uuid != bytea in the query
  // above. This happens because :businessId is treated as byte[] rather than UUID. Rather than work
  // out why, I changed the types to UUID to unblock folks. This seems to be an issue only for
  // native queries
  List<AccountActivity> findByReceiptId(UUID receiptId);

  Optional<AccountActivity> findByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<AccountActivityId> id);

  Optional<AccountActivity> findById(TypedId<AccountActivity> id);

  List<AccountActivity> findByAdjustmentId(TypedId<AdjustmentId> adjustmentId);

  List<AccountActivity> findByHoldId(TypedId<HoldId> holdId);

  List<AccountActivity> findByIntegrationSyncStatusAndBusinessId(
      AccountActivityIntegrationSyncStatus ready, TypedId<BusinessId> businessId);

  int countByIntegrationSyncStatusAndBusinessId(
      AccountActivityIntegrationSyncStatus ready, TypedId<BusinessId> businessId);

  @Modifying
  @Query(
      """
        update AccountActivity a set
          a.merchant.name = :name,
          a.merchant.statementDescriptor = :statementDescriptor,
          a.merchant.logoUrl = :logoUrl,
          a.merchant.codatSupplierId = :codatSupplierId,
          a.merchant.codatSupplierName = :codatSupplierName
        where a.businessId = :businessId
          and a.id = :activityId
      """)
  int updateMerchantData(
      @Param("businessId") TypedId<BusinessId> businessId,
      @Param("activityId") TypedId<AccountActivityId> activityId,
      @Param("name") String name,
      @Param("logoUrl") String logoUrl,
      @Param("statementDescriptor") String statementDescriptor,
      @Param("codatSupplierId") String codatId,
      @Param("codatSupplierName") String codatName);
}
