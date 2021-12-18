package com.clearspend.capital.controller.type.review;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Agent {

  @Sensitive
  @JsonProperty("email")
  String email;

  @JsonProperty("external_id")
  String externalId;
}
