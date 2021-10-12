package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class Summary {

  @JsonProperty("result")
  private String result;

  @JsonProperty("score")
  private Double score;

  @JsonProperty("tags")
  private List<String> tags;

  @JsonProperty("outcome_reasons")
  private List<String> outcomeReasons;

  @JsonProperty("outcome")
  private String outcome;

  private final Map<String, Object> additionalInfo = new HashMap<>();

  @JsonAnySetter
  public void addAdditionalInfo(String key, Object value) {
    additionalInfo.put(key, value);
  }
}
