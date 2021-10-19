package com.tranwall.capital.controller.type.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.FundingType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Program {

  @JsonProperty("programId")
  @NonNull
  private TypedId<ProgramId> programId;

  @Sensitive
  @JsonProperty("name")
  private String name;

  @Sensitive
  @JsonProperty("bin")
  private String bin;

  @JsonProperty("fundingType")
  private FundingType fundingType;

  public Program(com.tranwall.capital.data.model.Program program) {
    this.programId = program.getId();
    this.name = program.getName();
    this.bin = program.getBin();
    this.fundingType = program.getFundingType();
  }
}
