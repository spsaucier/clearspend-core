package com.clearspend.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class DashboardGraphData {

  @JsonProperty("totalSpend")
  @NonNull
  private BigDecimal totalSpend;

  @JsonProperty("averageSpend")
  @NonNull
  private BigDecimal averageSpend;

  @JsonProperty("graphData")
  private List<GraphData> graphData;
}
