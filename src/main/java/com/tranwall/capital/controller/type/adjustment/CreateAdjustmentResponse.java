package com.tranwall.capital.controller.type.adjustment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAdjustmentResponse {

  @JsonProperty("adjustmentId")
  @NonNull
  private UUID adjustmentId;
}
