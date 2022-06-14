package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.notification.AllocationNotificationSettingsResponse;
import com.clearspend.capital.controller.type.notification.AllocationNotificationsSettingRequest;
import com.clearspend.capital.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingsController {
  private final NotificationSettingsService notificationSettingsService;

  @GetMapping("/allocations/{allocationId}")
  AllocationNotificationSettingsResponse getAllocationNotificationSettings(
      @PathVariable("allocationId") final TypedId<AllocationId> allocationId) {
    return notificationSettingsService.getAllocationNotificationSetting(allocationId);
  }

  @PostMapping("/allocations/{allocationId}")
  AllocationNotificationSettingsResponse updateAllocationNotificationSettings(
      @PathVariable("allocationId") final TypedId<AllocationId> allocationId,
      @RequestBody final AllocationNotificationsSettingRequest request) {
    return notificationSettingsService.updateAllocationNotificationSetting(allocationId, request);
  }
}
