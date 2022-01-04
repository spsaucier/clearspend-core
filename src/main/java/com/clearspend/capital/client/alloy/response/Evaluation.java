package com.clearspend.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Evaluation {

  @JsonProperty("evaluation_token")
  String evaluationToken;

  @JsonProperty("timestamp")
  Timestamp timestamp;

  @JsonProperty("status")
  String status;

  @JsonProperty("application_name")
  String applicationName;

  @JsonProperty("outcome")
  String outcome;
}
