package com.tranwall.capital.controller.nonprod.type.twilio;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class KycPassRequest {

  @JsonProperty("to")
  String to;

  @JsonProperty("firstName")
  String firstName;
}
