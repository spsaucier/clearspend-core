package com.tranwall.capital.service.type;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocationChartData {

  TypedId<AllocationId> allocationId;
  String allocationName;
  Amount amount;

  public AllocationChartData(String name, Amount amount) {
    this.allocationName = name;
    this.amount = amount;
  }
}
