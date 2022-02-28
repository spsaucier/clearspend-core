package com.clearspend.capital.controller.type.chartOfAccounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class ChartOfAccountsMappingResponse {
  @JsonProperty("accountRef")
  @NonNull
  private String accountRef;

  @JsonProperty("categoryIconRef")
  @NonNull
  private Integer categoryIconRef;
}
