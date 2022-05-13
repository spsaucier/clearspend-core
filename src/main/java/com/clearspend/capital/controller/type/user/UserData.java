package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  @Sensitive
  @JsonProperty("firstName")
  @NonNull
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @NonNull
  private String lastName;

  @JsonProperty("archived")
  private boolean archived;

  public UserData(User user) {
    this.userId = user.getId();
    this.type = user.getType();
    this.firstName = user.getFirstName().getEncrypted();
    this.lastName = user.getLastName().getEncrypted();
    this.archived = user.isArchived();
  }
}
