package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAllocationRequest {

  @JsonProperty("programId")
  @NonNull
  @NotNull(message = "programId required")
  private TypedId<ProgramId> programId;

  @JsonProperty("name")
  @NonNull
  @NotNull(message = "name required")
  private String name;

  @JsonProperty("parentAllocationId")
  private TypedId<AllocationId> parentAllocationId;

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount required")
  private Amount amount;
}
