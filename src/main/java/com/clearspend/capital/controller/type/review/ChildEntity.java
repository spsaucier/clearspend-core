package com.clearspend.capital.controller.type.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
public class ChildEntity {

  @JsonProperty("entity_token")
  String entityToken;

  @JsonProperty("evaluation_tokens")
  List<String> evaluationTokens;
}
