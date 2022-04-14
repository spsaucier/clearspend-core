package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BusinessNotificationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.BusinessNotification;
import com.clearspend.capital.data.model.enums.BusinessNotificationType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BusinessNotificationRepository
    extends JpaRepository<BusinessNotification, TypedId<BusinessNotificationId>> {
  List<BusinessNotification> findAllByBusinessId(TypedId<BusinessId> businessId);

  Optional<BusinessNotification> findTopByBusinessIdAndUserIdAndTypeOrderByCreatedDesc(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, BusinessNotificationType type);

  @Query(
      "select b from BusinessNotification b where b.businessId = :businessId and b.created > :created and b.type in :types")
  List<BusinessNotification> findAllForBusinessSinceTime(
      TypedId<BusinessId> businessId, OffsetDateTime created, List<BusinessNotificationType> types);
}
