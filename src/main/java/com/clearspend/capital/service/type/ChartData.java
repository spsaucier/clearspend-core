package com.clearspend.capital.service.type;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@Getter
@With
@AllArgsConstructor
public class ChartData {

  private final List<MerchantCategoryChartData> merchantCategoryChartData;
  private final List<AllocationChartData> allocationChartData;
  private final List<UserChartData> userChartData;
  private final List<MerchantChartData> merchantChartData;
}
