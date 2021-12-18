package com.clearspend.capital.controller.nonprod.type.fusionauth;

import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class CreateBusinessOwnerRequest {

  @JsonProperty("businessId")
  TypedId<BusinessId> businessId;

  @JsonProperty("businessOwnerId")
  TypedId<BusinessOwnerId> businessOwnerId;

  @JsonProperty("username")
  String username;

  @JsonProperty("password")
  String password;
}
