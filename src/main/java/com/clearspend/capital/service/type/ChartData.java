package com.clearspend.capital.service.type;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartData {

  private List<MerchantCategoryChartData> merchantCategoryChartData;
  private List<AllocationChartData> allocationChartData;
  private List<UserChartData> userChartData;
  private List<MerchantChartData> merchantChartData;
}
