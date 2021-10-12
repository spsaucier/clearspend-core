package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
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
  private UUID allocationId;
}
