package com.tranwall.capital.client.i2c;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tranwall.capital.client.i2c.util.CardStatusCodeDeserializer;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CardStatus {

  @JsonProperty("code")
  @JsonDeserialize(using = CardStatusCodeDeserializer.class)
  CardStatusCode code;

  @JsonProperty("description")
  String description;
}
