package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
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
