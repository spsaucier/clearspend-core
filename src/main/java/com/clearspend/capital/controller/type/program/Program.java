package com.clearspend.capital.controller.type.program;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.ProgramId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.CardType;
import com.clearspend.capital.data.model.enums.FundingType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  @JsonProperty("cardType")
  private CardType cardType;

  public Program(com.clearspend.capital.data.model.Program program) {
    this.programId = program.getId();
    this.name = program.getName();
    this.bin = program.getBin();
    this.fundingType = program.getFundingType();
    this.cardType = program.getCardType();
  }
}
