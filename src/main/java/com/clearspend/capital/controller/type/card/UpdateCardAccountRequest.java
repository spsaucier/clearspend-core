package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UpdateCardAccountRequest {

  @JsonProperty("allocationId")
  @NonNull
  @Schema(example = "c9609768-647d-4f00-b755-e474cc761c33")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("accountId")
  @NonNull
  @Schema(example = "54826974-c2e3-4eee-a305-ba6f847748e8")
  private TypedId<AccountId> accountId;
}
