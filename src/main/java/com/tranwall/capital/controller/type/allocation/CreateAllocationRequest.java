package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAllocationRequest {

  @JsonProperty("name")
  @NonNull
  @NotNull(message = "name required")
  @Schema(example = "advertisement", description = "name of the department/ allocation")
  private String name;

  @NonNull
  @NotNull(message = "parent allocation id required")
  @JsonProperty("parentAllocationId")
  @Schema(example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<AllocationId> parentAllocationId;

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount required")
  @Schema(example = "{currency=USD,amount=100}")
  private Amount amount;
}
