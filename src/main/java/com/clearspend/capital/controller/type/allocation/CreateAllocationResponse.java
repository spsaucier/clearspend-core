package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAllocationResponse {

  @JsonProperty("allocationId")
  @NonNull
  @NotNull(message = "allocationId required")
  private TypedId<AllocationId> allocationId;
}
