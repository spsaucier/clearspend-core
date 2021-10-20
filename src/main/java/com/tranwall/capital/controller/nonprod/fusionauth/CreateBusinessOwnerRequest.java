package com.tranwall.capital.controller.nonprod.fusionauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Value;

@Value
public class CreateBusinessOwnerRequest {

  @JsonProperty("business_id")
  UUID businessId;

  @JsonProperty("business_owner_id")
  UUID businessOwnerId;

  @JsonProperty("username")
  String username;

  @JsonProperty("password")
  String password;
}
