package com.clearspend.capital.controller.type.business.prospect;

import static com.clearspend.capital.controller.type.Constants.EIN_PATTERN;
import static com.clearspend.capital.controller.type.Constants.PHONE_PATTERN;

import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Pattern;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConvertBusinessProspectRequest {

  @JsonProperty("legalName")
  @NonNull
  private String legalName;

  @JsonProperty("businessType")
  @NonNull
  private BusinessType businessType;

  @JsonProperty("employerIdentificationNumber")
  @NonNull
  @Pattern(regexp = EIN_PATTERN, message = "EIN should consist of 9 digits")
  private String employerIdentificationNumber;

  @JsonProperty("businessPhone")
  @NonNull
  @Schema(title = "Phone number in e.164 format", example = "+1234567890")
  @Pattern(regexp = PHONE_PATTERN, message = "Incorrect phone format.")
  private String businessPhone;

  @JsonProperty("address")
  @NonNull
  private Address address;
}
