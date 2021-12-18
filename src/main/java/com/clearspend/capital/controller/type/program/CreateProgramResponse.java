package com.clearspend.capital.controller.type.program;

import com.clearspend.capital.common.typedid.data.ProgramId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
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
