package com.clearspend.capital.client.codat.webhook.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatWebhookRequest {
  @JsonProperty("CompanyId")
  @NonNull
  private String companyId;

  @JsonProperty("RuleType")
  @NonNull
  private String ruleType;

  @JsonProperty("Data")
  @NonNull
  private CodatWebhookDataType data;
}
