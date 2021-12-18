package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class ForgotPasswordRequest {

  @JsonProperty("email")
  @Sensitive
  private @NonNull String email;
}
