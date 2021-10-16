package com.tranwall.capital.controller.type.business.prospect;

import static com.tranwall.capital.controller.type.Constants.PHONE_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Pattern;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SetBusinessProspectPhoneRequest {

  @Sensitive
  @JsonProperty("phone")
  @NonNull
  @Schema(title = "Phone number in e.164 format", example = "+1234567890")
  @Pattern(regexp = PHONE_PATTERN, message = "Incorrect phone format.")
  private String phone;
}
