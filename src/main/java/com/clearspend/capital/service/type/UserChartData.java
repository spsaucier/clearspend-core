package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserChartData {

  TypedId<UserId> userId;
  UserType type;
  byte[] firstNameEncrypted;
  String firstName;
  byte[] lastNameEncrypted;
  String lastName;
  Amount amount;

  public UserChartData(String name, Amount amount) {
    this.firstName = name;
    this.amount = amount;
  }

  public UserChartData(
      TypedId<UserId> userId,
      UserType type,
      byte[] firstNameEncrypted,
      byte[] lastNameEncrypted,
      Amount amount) {
    this.userId = userId;
    this.type = type;
    this.firstNameEncrypted = firstNameEncrypted;
    this.lastNameEncrypted = lastNameEncrypted;
    this.amount = amount;
  }
}
