package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Evaluation {

  @JsonProperty("evaluation_token")
  String evaluationToken;

  @JsonProperty("status")
  String status;

  @JsonProperty("application_name")
  String applicationName;

  @JsonProperty("outcome")
  String outcome;
}
