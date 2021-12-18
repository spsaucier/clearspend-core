package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.enums.MerchantType;
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
