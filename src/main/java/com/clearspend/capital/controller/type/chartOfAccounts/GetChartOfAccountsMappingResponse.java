package com.clearspend.capital.controller.type.chartOfAccounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class GetChartOfAccountsMappingResponse {
  @JsonProperty("results")
  @NonNull
  private List<ChartOfAccountsMappingResponse> results;
}
