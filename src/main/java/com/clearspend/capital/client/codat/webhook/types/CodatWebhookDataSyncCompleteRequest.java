package com.clearspend.capital.client.codat.webhook.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatWebhookDataSyncCompleteRequest {
  @JsonProperty("CompanyId")
  @NonNull
  private String companyId;

  @JsonProperty("Data")
  @NonNull
  private CodatWebhookSyncData data;
}