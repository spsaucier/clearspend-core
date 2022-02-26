package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateAllocationBalanceResponse {

  @JsonProperty("adjustmentId")
  @NonNull
  private TypedId<AdjustmentId> adjustmentId;

  @JsonProperty("ledgerBalance")
  @NonNull
  private Amount ledgerBalance;
}
