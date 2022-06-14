package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.AllocationNotificationSettingId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.notifications.AllocationNotificationsSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationNotificationSettingRepository
    extends JpaRepository<
        AllocationNotificationsSettings, TypedId<AllocationNotificationSettingId>> {
  Optional<AllocationNotificationsSettings> findByAllocationId(
      final TypedId<AllocationId> allocationId);

  void deleteByAllocationId(final TypedId<AllocationId> allocationId);
}
