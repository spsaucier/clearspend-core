package com.tranwall.capital.client.i2c.push.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HealthCheckRequest {

  @JsonProperty("Header")
  private Header header;

  @JsonProperty("HealthCheckId")
  private String healthCheckId;
}
