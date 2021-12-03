package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import lombok.Value;

@Value
public class Notes {

  @JsonProperty("note")
  String note;

  @JsonProperty("note_author_agent_email")
  String noteAuthorAgentEmail;

  @JsonProperty("created_at")
  Timestamp createdAt;

  @JsonProperty("updated_at")
  Timestamp updatedAt;
}
