package com.tranwall.capital.client.i2c.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddCardResponseRoot {

  @JsonProperty("addCardResponse")
  private final AddCardResponse response;
}
