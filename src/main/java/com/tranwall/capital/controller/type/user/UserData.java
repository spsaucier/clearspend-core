package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UserData {

  @JsonProperty("userId")
  @NonNull
  private TypedId<UserId> userId;

  @JsonProperty("type")
  @NonNull
  private UserType type;

  @JsonProperty("firstName")
  @NonNull
  private String firstName;

  @JsonProperty("lastName")
  @NonNull
  private String lastName;

  public UserData(com.tranwall.capital.data.model.User user) {
    this.userId = user.getId();
    this.type = user.getType();
    this.firstName = user.getFirstName().getEncrypted();
    this.lastName = user.getLastName().getEncrypted();
  }
}
