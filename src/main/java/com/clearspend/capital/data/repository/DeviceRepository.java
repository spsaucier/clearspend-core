package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.DeviceRegistrationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.DeviceRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository
    extends JpaRepository<DeviceRegistration, TypedId<DeviceRegistrationId>> {
  Optional<DeviceRegistration> findAllByUserId(TypedId<UserId> userId);
}
