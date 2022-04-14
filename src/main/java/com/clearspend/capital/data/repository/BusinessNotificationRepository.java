package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BusinessNotificationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.BusinessNotification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessNotificationRepository
    extends JpaRepository<BusinessNotification, TypedId<BusinessNotificationId>> {
  List<BusinessNotification> findAllByBusinessId(TypedId<BusinessId> businessId);
}
