package com.clearspend.capital.client.codat.webhook.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatWebhookConnectionChangedData {
  @JsonProperty("dataConnectionId")
  @NonNull
  private String dataConnectionId;

  @JsonProperty("newStatus")
  @NonNull
  private String newStatus;
}
