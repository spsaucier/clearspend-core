package com.clearspend.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class Summary {

  @JsonProperty("result")
  String result;

  @JsonProperty("score")
  Double score;

  @JsonProperty("tags")
  List<String> tags;

  @JsonProperty("outcome_reasons")
  List<String> outcomeReasons;

  @JsonProperty("outcome")
  String outcome;

  Map<String, Object> additionalInfo = new HashMap<>();

  @JsonAnySetter
  public void addAdditionalInfo(String key, Object value) {
    additionalInfo.put(key, value);
  }
}
