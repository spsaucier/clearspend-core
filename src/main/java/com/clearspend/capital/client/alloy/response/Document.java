package com.clearspend.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import lombok.Value;

@Value
public class Document {

  @JsonProperty("document_token")
  String documentToken;

  @JsonProperty("timestamp")
  Timestamp timestamp;

  @JsonProperty("type")
  String type;

  @JsonProperty("name")
  String name;
}
