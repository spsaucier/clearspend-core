package com.tranwall.capital.client.alloy.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
public class OnboardResponse {

  @JsonProperty("status_code")
  private Integer statusCode;

  @JsonProperty("error")
  private String error;

  @JsonProperty("timestamp")
  private Long timestamp;

  @JsonProperty("evaluation_token")
  private String evaluationToken;

  @JsonProperty("entity_token")
  private String entityToken;

  @JsonProperty("parent_entity_token")
  private String parentEntityToken;

  @JsonProperty("application_token")
  private String applicationToken;

  @JsonProperty("application_version_id")
  private Integer applicationVersionId;

  @JsonProperty("summary")
  private Summary summary;

  @JsonProperty("required")
  private List<Required> required;

  private final Map<String, Object> additionalInfo = new HashMap<>();

  @JsonAnySetter
  public void addAdditionalInfo(String key, Object value) {
    additionalInfo.put(key, value);
  }
}
