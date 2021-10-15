package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AllocationFundCardResponse {

  @JsonProperty("businessAdjustmentId")
  @NonNull
  private TypedId<AdjustmentId> businessAdjustmentId;

  @JsonProperty("businessLedgerBalance")
  @NonNull
  private Amount businessLedgerBalance;

  @JsonProperty("allocationAdjustmentId")
  @NonNull
  private TypedId<AdjustmentId> allocationAdjustmentId;

  @JsonProperty("allocationLedgerBalance")
  @NonNull
  private Amount allocationLedgerBalance;
}
