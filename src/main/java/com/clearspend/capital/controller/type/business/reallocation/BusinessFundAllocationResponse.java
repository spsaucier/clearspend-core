package com.clearspend.capital.controller.type.business.reallocation;

import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BusinessFundAllocationResponse {

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
