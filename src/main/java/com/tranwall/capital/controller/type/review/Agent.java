package com.tranwall.capital.controller.type.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import lombok.Value;

@Value
public class Agent {

  @Sensitive
  @JsonProperty("email")
  String email;

  @JsonProperty("external_id")
  String externalId;
}
