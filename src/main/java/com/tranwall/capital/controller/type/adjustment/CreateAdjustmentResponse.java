package com.tranwall.capital.controller.type.adjustment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAdjustmentResponse {

  @JsonProperty("adjustmentId")
  @NonNull
  private TypedId<AdjustmentId> adjustmentId;
}
