package com.clearspend.capital.client.i2c.push.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HealthCheckResponse {

  @JsonProperty("ResponseCode")
  private String responseCode;

  @JsonProperty("HealthCheckId")
  private String healthCheckId;
}
