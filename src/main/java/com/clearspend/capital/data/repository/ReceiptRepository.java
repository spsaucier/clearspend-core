package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Receipt;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptRepository extends JpaRepository<Receipt, TypedId<ReceiptId>> {

  Optional<Receipt> findReceiptByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<ReceiptId> id);

  @Query(
      "SELECT r FROM Receipt r WHERE r.businessId = :businessId AND r.uploadUserId = :userId AND r.linked = false")
  List<Receipt> findReceiptByBusinessIdAndUserIdAndUnLinked(
      @Param("businessId") final TypedId<BusinessId> businessId,
      @Param("userId") final TypedId<UserId> userId);

  List<Receipt> findAllByIdIn(final Set<TypedId<ReceiptId>> receiptIds);
}
