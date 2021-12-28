package com.clearspend.capital.service.type;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DashboardData {

  BigDecimal totalAmount;
  BigDecimal averageAmount;
  List<GraphData> graphData;
}
