package com.tranwall.capital.controller.nonprod.fusionauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Value;

@Value
public class CreateUserRequest {

  @JsonProperty("business_id")
  UUID businessId;

  @JsonProperty("user_id")
  UUID userId;

  @JsonProperty("username")
  String username;

  @JsonProperty("password")
  String password;
}
