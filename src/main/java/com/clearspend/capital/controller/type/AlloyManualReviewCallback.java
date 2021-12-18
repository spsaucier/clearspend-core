package com.clearspend.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
public class AlloyManualReviewCallback {

  @JsonProperty("entity_token")
  String entityToken;

  @JsonProperty("external_entity_id")
  Object externalEntityId;

  @JsonProperty("group_token")
  Object groupToken;

  @JsonProperty("external_group_id")
  Object externalGroupId;

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
  List<String> reasons = null;

  @JsonProperty("started")
  Long started;

  @JsonProperty("timestamp")
  Long timestamp;

  @JsonProperty("completed")
  Long completed;

  @JsonProperty("reviewer")
  String reviewer;

  @JsonProperty("agent")
  List<Object> notes = null;

  @JsonProperty("child_entities")
  List<Object> childEntities = null;
}
