package com.clearspend.capital.controller.nonprod.type.fusionauth;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
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

  @Sensitive
  @JsonProperty("password")
  String password;
}
