package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BusinessNotificationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.BusinessNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessNotificationRepository
    extends JpaRepository<BusinessNotification, TypedId<BusinessNotificationId>> {}
