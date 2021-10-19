package com.tranwall.capital.controller.type.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.data.model.enums.FundingType;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
  @Enumerated(EnumType.STRING)
  private FundingType fundingType;
}
