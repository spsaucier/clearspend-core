package com.tranwall.capital.service.type;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserChartData {

  TypedId<UserId> userId;
  UserType type;
  String firstName;
  String lastName;
  Amount amount;

  public UserChartData(String name, Amount amount) {
    this.firstName = name;
    this.amount = amount;
  }
}
