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

  @JsonProperty("adjustmentIdFrom")
  @NonNull
  private TypedId<AdjustmentId> adjustmentIdFrom;

  @JsonProperty("ledgerBalanceFrom")
  @NonNull
  private Amount ledgerBalanceFrom;

  @JsonProperty("adjustmentIdTo")
  @NonNull
  private TypedId<AdjustmentId> adjustmentIdTo;

  @JsonProperty("ledgerBalanceTo")
  @NonNull
  private Amount ledgerBalanceTo;
}
