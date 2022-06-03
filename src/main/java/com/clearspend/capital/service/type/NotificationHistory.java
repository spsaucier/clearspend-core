package com.clearspend.capital.service.type;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationHistory {

  private String businessId;

  private String userId;

  private OffsetDateTime notificationDate;

  private String accountActivityId;

  private String message;
}
