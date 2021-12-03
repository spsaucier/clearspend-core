package com.tranwall.capital.client.alloy.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescribeDocumentRequest {

  @NonNull
  @JsonProperty("name")
  private String name;

  @NonNull
  @JsonProperty("extension")
  private DocumentExtension extension;

  @NonNull
  @JsonProperty("type")
  private DocumentType type;

  @JsonProperty("note")
  private String note;

  @JsonProperty("note_author_agent_email")
  private String noteAuthorAgentEmail;
}
