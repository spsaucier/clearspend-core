package com.clearspend.capital.controller.type.business.prospect;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ValidateBusinessProspectIdentifierRequest {

  public enum IdentifierType {
    EMAIL,
    PHONE,
  }

  @Sensitive
  @NonNull
  @JsonProperty("identifierType")
  @Schema(title = "Type of Identifier to validate", example = "EMAIL")
  private IdentifierType identifierType;

  @Sensitive
  @NonNull
  @JsonProperty("otp")
  @Schema(title = "OTP received via email/phone", example = "67890")
  private String otp;
}
