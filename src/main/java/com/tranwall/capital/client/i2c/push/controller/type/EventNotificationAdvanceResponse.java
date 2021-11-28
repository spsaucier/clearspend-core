package com.tranwall.capital.client.i2c.push.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventNotificationAdvanceResponse {

  @JsonProperty("ResponseCode")
  private String responseCode;

  @JsonProperty("NotificationEventId")
  private String notificationEventRef;
}
