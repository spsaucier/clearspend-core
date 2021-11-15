package com.tranwall.capital.client.i2c.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseI2CResponse {

  @JsonProperty("responseCode")
  private String responseCode;

  @JsonProperty("responseDesc")
  private String responseDesc;
}
