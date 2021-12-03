package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Required {

  @JsonProperty("key")
  String key;

  @JsonProperty("type")
  String type;

  @JsonProperty("description")
  String description;

  @JsonProperty("regex")
  String regex;

  @JsonProperty("message")
  String message;
}
