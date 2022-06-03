package com.clearspend.capital.controller.type.notification;

import com.clearspend.capital.service.type.NotificationHistory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class NotificationHistoryResponse {

  @JsonProperty("businessId")
  @NonNull
  private String businessId;

  @JsonProperty("userId")
  @NonNull
  private String userId;

  @JsonProperty("accountActivityId")
  @NonNull
  private String accountActivityId;

  @JsonProperty("message")
  @NonNull
  private String message;

  public static NotificationHistoryResponse of(NotificationHistory notificationHistory) {
    NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
    notificationHistoryResponse.setUserId(notificationHistory.getUserId());
    notificationHistoryResponse.setBusinessId(notificationHistory.getBusinessId());
    notificationHistoryResponse.setAccountActivityId(notificationHistory.getAccountActivityId());
    notificationHistoryResponse.setMessage(notificationHistory.getMessage());
    return notificationHistoryResponse;
  }
}
