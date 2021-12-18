package com.clearspend.capital.controller.type.program;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.data.model.enums.CardType;
import com.clearspend.capital.data.model.enums.FundingType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateProgramRequest {

  @Sensitive
  @JsonProperty("bin")
  private String bin;

  @Sensitive
  @JsonProperty("name")
  private String name;

  @JsonProperty("fundingType")
  @NonNull
  private FundingType fundingType;

  @JsonProperty("cardType")
  @NonNull
  private CardType cardType;

  @Sensitive
  @NonNull
  @JsonProperty("i2c_program_ref")
  private String i2cProgramRef;
}
