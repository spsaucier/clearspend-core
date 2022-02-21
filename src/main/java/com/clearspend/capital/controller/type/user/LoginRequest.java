package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class LoginRequest {

  @JsonProperty("username")
  private @NonNull String username;

  @Sensitive
  @JsonProperty("password")
  private @NonNull String password;
}
