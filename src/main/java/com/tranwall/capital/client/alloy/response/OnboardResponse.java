package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class OnboardResponse {

  @JsonProperty("status_code")
  Integer statusCode;

  @JsonProperty("error")
  String error;

  @JsonProperty("timestamp")
  Long timestamp;

  @JsonProperty("evaluation_token")
  String evaluationToken;

  @JsonProperty("entity_token")
  String entityToken;

  @JsonProperty("parent_entity_token")
  String parentEntityToken;

  @JsonProperty("application_token")
  String applicationToken;

  @JsonProperty("application_version_id")
  Integer applicationVersionId;

  @JsonProperty("summary")
  Summary summary;

  @JsonProperty("required")
  List<Required> required;

  Map<String, Object> additionalInfo = new HashMap<>();

  @JsonAnySetter
  public void addAdditionalInfo(String key, Object value) {
    additionalInfo.put(key, value);
  }
}
