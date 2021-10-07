package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Required {

  @JsonProperty("key")
  private String key;

  @JsonProperty("type")
  private String type;

  @JsonProperty("description")
  private String description;

  @JsonProperty("regex")
  private String regex;

  @JsonProperty("message")
  private String message;
}
