package com.clearspend.capital.service.notification;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.DeviceRegistration;
import com.clearspend.capital.data.repository.DeviceRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class DeviceRegistrationService {
  private final DeviceRepository deviceRepository;

  public void saveUpdateDeviceTokens(TypedId<UserId> userId, List<String> registrationTokens) {
    DeviceRegistration deviceRegistration =
        deviceRepository.findAllByUserId(userId).orElse(new DeviceRegistration());
    Set<String> deviceTokens = new HashSet<>();
    if (deviceRegistration.getDeviceIds() != null) {
      deviceTokens.addAll(Arrays.asList(deviceRegistration.getDeviceIds()));
    }
    deviceTokens.addAll(registrationTokens);
    deviceRegistration.setUserId(userId);
    deviceRegistration.setDeviceIds(deviceTokens.toArray(new String[0]));
    deviceRepository.save(deviceRegistration);
    log.info("Saved device {} for user {}.", registrationTokens, userId);
  }

  public void removeDeviceToken(TypedId<UserId> userId, List<String> registrationTokens) {
    Optional<DeviceRegistration> deviceRegistration = deviceRepository.findAllByUserId(userId);
    deviceRegistration.ifPresent(
        registration -> {
          registration.setDeviceIds(
              Arrays.stream(registration.getDeviceIds())
                  .filter(s -> !registrationTokens.contains(s))
                  .distinct()
                  .toArray(String[]::new));
          if (registration.getDeviceIds().length < 1) {
            deviceRepository.deleteById(registration.getId());
          } else {
            deviceRepository.save(registration);
          }
        });
    log.info("Removed device {} for user {}.", registrationTokens, userId);
  }

  Optional<DeviceRegistration> findAllByUserId(TypedId<UserId> userId) {
    return deviceRepository.findAllByUserId(userId);
  }
}
