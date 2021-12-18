package com.clearspend.capital.controller.type.adjustment;

import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
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
