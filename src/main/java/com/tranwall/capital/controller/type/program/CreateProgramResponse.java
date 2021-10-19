package com.tranwall.capital.controller.type.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateProgramResponse {

  @JsonProperty("programId")
  @NonNull
  @NotNull(message = "programId required")
  private TypedId<ProgramId> programId;
}
