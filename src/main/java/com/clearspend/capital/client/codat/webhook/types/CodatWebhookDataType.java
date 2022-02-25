package com.clearspend.capital.client.codat.webhook.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatWebhookDataType {
  @JsonProperty("dataType")
  @NonNull
  private String dataType;

  @JsonProperty("datasetId")
  @NonNull
  private String datasetId;
}