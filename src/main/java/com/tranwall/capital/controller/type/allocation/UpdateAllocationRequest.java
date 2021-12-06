package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateAllocationRequest {

  @JsonProperty("name")
  @Schema(example = "advertisement", description = "name of the department/ allocation")
  private String name;

  @JsonProperty("parentAllocationId")
  @Schema(example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<AllocationId> parentAllocationId;

  @JsonProperty("ownerId")
  private TypedId<UserId> ownerId;
}
