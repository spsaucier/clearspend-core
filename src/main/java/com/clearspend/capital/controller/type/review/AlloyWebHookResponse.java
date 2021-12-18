package com.clearspend.capital.controller.type.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class AlloyWebHookResponse {

  @JsonProperty("request_token")
  String requestToken;

  @JsonProperty("timestamp")
  String timestamp;

  @JsonProperty("type")
  String type;

  @JsonProperty("description")
  String description;

  @JsonProperty("data")
  Data data;
}
