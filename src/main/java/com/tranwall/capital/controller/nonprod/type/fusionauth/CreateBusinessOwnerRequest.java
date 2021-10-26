package com.tranwall.capital.controller.nonprod.type.fusionauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
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
