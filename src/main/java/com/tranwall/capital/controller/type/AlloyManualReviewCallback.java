package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class AlloyManualReviewCallback {

  @JsonProperty("entity_token")
  private String entityToken;

  @JsonProperty("external_entity_id")
  private Object externalEntityId;

  @JsonProperty("group_token")
  private Object groupToken;

  @JsonProperty("external_group_id")
  private Object externalGroupId;

  @JsonProperty("review_token")
  private String reviewToken;

  @JsonProperty("application_token")
  private String applicationToken;

  @JsonProperty("application_name")
  private String applicationName;

  @JsonProperty("outcome")
  private String outcome;

  @JsonProperty("reason")
  private String reason;

  @JsonProperty("reasons")
  private List<String> reasons = null;

  @JsonProperty("started")
  private Long started;

  @JsonProperty("timestamp")
  private Long timestamp;

  @JsonProperty("completed")
  private Long completed;

  @JsonProperty("reviewer")
  private String reviewer;

  @JsonProperty("agent")
  private List<Object> notes = null;

  @JsonProperty("child_entities")
  private List<Object> childEntities = null;
}
