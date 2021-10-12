package com.tranwall.capital.controller.type.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.data.model.enums.FundingType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Program {

  @JsonProperty("programId")
  @NonNull
  private UUID programId;

  @Sensitive
  @JsonProperty("name")
  private String name;

  @Sensitive
  @JsonProperty("bin")
  private String bin;

  @JsonProperty("fundingType")
  private FundingType fundingType;
}
