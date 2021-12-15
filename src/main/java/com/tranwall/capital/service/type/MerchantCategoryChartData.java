package com.tranwall.capital.service.type;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.enums.MerchantType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantCategoryChartData {

  MerchantType merchantType;
  Amount amount;
}
