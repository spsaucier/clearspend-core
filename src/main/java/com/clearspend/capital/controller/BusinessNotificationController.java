package com.clearspend.capital.controller;

import com.clearspend.capital.data.model.BusinessNotification;
import com.clearspend.capital.service.BusinessNotificationService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-notification")
@RequiredArgsConstructor
@Slf4j
public class BusinessNotificationController {
  private final BusinessNotificationService businessNotificationService;

  @GetMapping("/chart-of-accounts")
  List<BusinessNotification> getChartOfAccountsNotifications() {
    return businessNotificationService.getUnseenNotificationsForUser(
        CurrentUser.getBusinessId(), CurrentUser.get().userId());
  }

  @PostMapping("/accept-chart-of-accounts")
  BusinessNotification acceptChartOfAccountChangesForUser() {
    return businessNotificationService.acceptChartOfAccountChangesForUser(
        CurrentUser.getBusinessId(), CurrentUser.get().userId());
  }

  @GetMapping("/chart-of-accounts/recent")
  List<BusinessNotification> getRecentChartOfAccountsNotifications() {
    return businessNotificationService.getRecentChartOfAccountsNotifications(
        CurrentUser.getBusinessId());
  }
}
