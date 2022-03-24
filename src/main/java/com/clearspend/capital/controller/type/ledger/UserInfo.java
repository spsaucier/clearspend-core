package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

  @JsonProperty("userId")
  @NonNull
  private TypedId<UserId> userId;

  @Sensitive
  @JsonProperty("firstName")
  @NonNull
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @NonNull
  private String lastName;

  @Sensitive
  @JsonProperty("email")
  @NonNull
  private String email;
}
