package com.clearspend.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardGraphData {

  @JsonProperty("totalSpend")
  private BigDecimal totalSpend;

  @JsonProperty("averageSpend")
  private BigDecimal averageSpend;

  @JsonProperty("graphData")
  private List<GraphData> graphData;
}
