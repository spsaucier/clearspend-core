package com.clearspend.capital.controller.type.review;

import com.clearspend.capital.client.alloy.response.Notes;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
public class Data {

  @JsonProperty("service")
  String service;

  @JsonProperty("entity_token")
  String entityToken;

  @JsonProperty("external_entity_id")
  String externalEntityId;

  @JsonProperty("group_token")
  String groupToken;

  @JsonProperty("external_group_id")
  String externalGroupId;

  @JsonProperty("review_token")
  String reviewToken;

  @JsonProperty("application_token")
  String applicationToken;

  @JsonProperty("application_name")
  String applicationName;

  @JsonProperty("outcome")
  String outcome;

  @JsonProperty("reason")
  String reason;

  @JsonProperty("reasons")
  List<String> reasons;

  @JsonProperty("started")
  String started;

  @JsonProperty("timestamp")
  String timestamp;

  @JsonProperty("completed")
  String completed;

  @JsonProperty("reviewer")
  String reviewer;

  @JsonProperty("agent")
  Agent agent;

  @JsonProperty("notes")
  List<Notes> notes;

  @JsonProperty("child_entities")
  List<ChildEntity> childEntities;
}
