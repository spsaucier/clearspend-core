package com.clearspend.capital.client.alloy.response;

import com.clearspend.capital.client.alloy.request.DocumentExtension;
import com.clearspend.capital.client.alloy.request.DocumentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import java.util.List;
import lombok.Value;

@Value
public class DocumentResponse {

  @JsonProperty("document_token")
  String documentToken;

  @JsonProperty("type")
  DocumentType type;

  @JsonProperty("name")
  String name;

  @JsonProperty("extension")
  DocumentExtension extension;

  @JsonProperty("uploaded")
  Boolean uploaded;

  @JsonProperty("timestamp")
  Timestamp timestamp;

  @JsonProperty("approved")
  Boolean approved;

  @JsonProperty("approval_agent_email")
  String approvalAgentEmail;

  @JsonProperty("approval_timestamp")
  Timestamp approvalTimestamp;

  @JsonProperty("notes")
  List<Notes> notes;
}
