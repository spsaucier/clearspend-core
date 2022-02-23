package com.clearspend.capital.controller.type.business;

import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateBusiness {

  @JsonProperty("legalName")
  private String legalName;

  @JsonProperty("businessType")
  private BusinessType businessType;

  @JsonProperty("employerIdentificationNumber")
  private String employerIdentificationNumber;

  @JsonProperty("businessPhone")
  @Schema(title = "Phone number in e.164 format", example = "+1234567890")
  private String businessPhone;

  @JsonProperty("address")
  private Address address;

  @JsonProperty("merchantType")
  private MerchantType merchantType;

  @JsonProperty("description")
  private String description;

  @JsonProperty("url")
  private String url;
}
