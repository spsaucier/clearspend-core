package com.clearspend.capital.controller.type.chartOfAccounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AddChartOfAccountsMappingRequest {
  @JsonProperty("accountRef")
  @NonNull
  private String accountRef;

  @JsonProperty("categoryIconRef")
  @NonNull
  private Integer categoryIconRef;
}
