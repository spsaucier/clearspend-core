package com.tranwall.capital.controller.type.business.prospect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
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
  @Schema(name = "Type of Identifier to validate", example = "EMAIL")
  private IdentifierType identifierType;

  @Sensitive
  @NonNull
  @JsonProperty("otp")
  @Schema(name = "OTP received via email/phone", example = "67890")
  private String otp;
}
