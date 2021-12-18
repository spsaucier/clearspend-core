package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.service.type.UserChartData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserChartInfo {

  @JsonProperty("userId")
  private TypedId<UserId> userId;

  @JsonProperty("type")
  private UserType type;

  @Sensitive
  @JsonProperty("firstName")
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  private String lastName;

  public UserChartInfo(UserChartData user) {
    this.userId = user.getUserId();
    this.type = user.getType();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
  }
}
