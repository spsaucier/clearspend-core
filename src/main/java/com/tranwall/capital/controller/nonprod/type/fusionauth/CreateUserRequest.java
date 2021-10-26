package com.tranwall.capital.controller.nonprod.type.fusionauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import lombok.Value;

@Value
public class CreateUserRequest {

  @JsonProperty("businessId")
  TypedId<BusinessId> businessId;

  @JsonProperty("userId")
  TypedId<UserId> userId;

  @JsonProperty("username")
  String username;

  @JsonProperty("password")
  String password;
}
