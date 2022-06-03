package com.clearspend.capital.controller.type.notification.firebase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class DeviceTokenRegistrationRequest {

  @JsonProperty("deviceToken")
  @NonNull
  private String deviceToken;
}
