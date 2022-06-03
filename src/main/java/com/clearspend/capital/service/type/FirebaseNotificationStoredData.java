package com.clearspend.capital.service.type;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FirebaseNotificationStoredData {

  String accountActivityId;
  String body;
  String notificationIds;
}
