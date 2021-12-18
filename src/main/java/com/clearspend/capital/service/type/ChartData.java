package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.type.activity.ChartFilterType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MerchantType;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartData {

  public static final String DEFAULT_NAME = "Others";

  private List<MerchantCategoryChartData> merchantCategoryChartData;
  private List<AllocationChartData> allocationChartData;
  private List<UserChartData> userChartData;
  private List<MerchantChartData> merchantChartData;

  public void addOtherCategory(ChartFilterType chartFilterType, BigDecimal amount) {
    switch (chartFilterType) {
      case MERCHANT_CATEGORY -> this.merchantCategoryChartData.add(
          new MerchantCategoryChartData(MerchantType.OTHERS, new Amount(Currency.USD, amount)));
      case ALLOCATION -> this.allocationChartData.add(
          new AllocationChartData(DEFAULT_NAME, new Amount(Currency.USD, amount)));
      case MERCHANT -> this.merchantChartData.add(
          new MerchantChartData(DEFAULT_NAME, new Amount(Currency.USD, amount)));
      case EMPLOYEE -> this.userChartData.add(
          new UserChartData(DEFAULT_NAME, new Amount(Currency.USD, amount)));
    }
  }
}
