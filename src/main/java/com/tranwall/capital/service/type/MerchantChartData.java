package com.tranwall.capital.service.type;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.enums.MerchantType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantChartData {

  Amount amount;
  Integer categoryCode;
  MerchantType merchantType;
  String name;
  String number;

  public MerchantChartData(String name, Amount amount) {
    this.name = name;
    this.amount = amount;
  }
}
