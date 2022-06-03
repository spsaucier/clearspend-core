package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.notification.NotificationHistoryResponse;
import com.clearspend.capital.service.notification.NotificationAuditService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationAuditController {

  private final NotificationAuditService notificationAuditService;

  @GetMapping
  List<NotificationHistoryResponse> retrieveNotificationHistory(
      @RequestParam(value = "limit", required = false, defaultValue = "1")
          @Parameter(
              name = "limit",
              description = "Number of days to query bigdata table.",
              example = "5")
          Integer limit) {
    return notificationAuditService
        .retrieveNotificationHistoryForUser(
            CurrentUser.getActiveBusinessId(), CurrentUser.getUserId(), limit)
        .stream()
        .map(NotificationHistoryResponse::of)
        .toList();
  }
}
