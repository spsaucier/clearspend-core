package com.clearspend.capital.controller.nonprod.type.fusionauth;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class CreateUserRequest {

  @JsonProperty("businessId")
  TypedId<BusinessId> businessId;

  @JsonProperty("userId")
  TypedId<UserId> userId;

  @JsonProperty("username")
  String username;

  @Sensitive
  @JsonProperty("password")
  String password;
}
