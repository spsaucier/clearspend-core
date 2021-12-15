package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.type.UserChartData;
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
