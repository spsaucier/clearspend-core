package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.notification.firebase.DeviceTokenRegistrationRequest;
import com.clearspend.capital.service.notification.DeviceRegistrationService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device/provision/device-tokens")
@RequiredArgsConstructor
public class DeviceProvisioningController {

  private final DeviceRegistrationService deviceRegistrationService;

  @PostMapping
  void saveDeviceToken(
      @Validated @RequestBody DeviceTokenRegistrationRequest deviceTokenRegistrationRequest) {
    deviceRegistrationService.saveUpdateDeviceTokens(
        CurrentUser.getUserId(), List.of(deviceTokenRegistrationRequest.getDeviceToken()));
  }

  @DeleteMapping
  void removeDeviceToken(
      @Validated @RequestBody DeviceTokenRegistrationRequest deviceTokenRegistrationRequest) {
    deviceRegistrationService.removeDeviceToken(
        CurrentUser.getUserId(), List.of(deviceTokenRegistrationRequest.getDeviceToken()));
  }
}
