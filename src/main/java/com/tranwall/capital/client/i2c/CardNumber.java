package com.tranwall.capital.client.i2c;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CardNumber {
  @JsonProperty("number")
  private String number;

  @JsonProperty("expiryDate")
  private String expiryDate;
}
