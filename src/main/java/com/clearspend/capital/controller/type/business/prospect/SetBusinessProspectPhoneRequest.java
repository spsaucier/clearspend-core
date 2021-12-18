package com.clearspend.capital.controller.type.business.prospect;

import static com.clearspend.capital.controller.type.Constants.PHONE_PATTERN;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
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
