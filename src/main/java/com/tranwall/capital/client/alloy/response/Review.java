package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import lombok.Value;

@Value
public class Review {

  @JsonProperty("review_token")
  String reviewToken;

  @JsonProperty("outcome")
  String outcome;

  @JsonProperty("reason")
  String reason;

  @JsonProperty("application_name")
  String applicationName;

  @JsonProperty("timestamp")
  Timestamp timestamp;

  @JsonProperty("reviewer")
  String reviewer;
}
