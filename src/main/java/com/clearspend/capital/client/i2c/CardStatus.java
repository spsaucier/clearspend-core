package com.clearspend.capital.client.i2c;

import com.clearspend.capital.client.i2c.util.CardStatusCodeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
