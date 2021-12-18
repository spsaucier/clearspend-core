package com.clearspend.capital.controller.type.business.prospect;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SetBusinessProspectPhoneResponse {

  @Sensitive
  @JsonProperty("otp")
  @Schema(
      name = "The OTP that was sent to the user but only in a non-production environment",
      example = "567890")
  private String otp;
}
