package com.clearspend.capital.controller.nonprod.type.twilio;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
public class KycFailRequest {

  @JsonProperty("to")
  String to;

  @JsonProperty("firstName")
  String firstName;

  @JsonProperty("reasons")
  List<String> reasons;
}
