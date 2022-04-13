package com.clearspend.capital.data.model.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BusinessNotificationData {
  @JsonProperty("oldValue")
  private String oldValue;

  @JsonProperty("newValue")
  private String newValue;
}
