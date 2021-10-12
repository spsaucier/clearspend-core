package com.tranwall.capital.controller.type.business.reallocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BusinessFundAllocationResponse {

  @JsonProperty("businessAdjustmentId")
  @NonNull
  private UUID businessAdjustmentId;

  @JsonProperty("businessLedgerBalance")
  @NonNull
  private Amount businessLedgerBalance;

  @JsonProperty("allocationAdjustmentId")
  @NonNull
  private UUID allocationAdjustmentId;

  @JsonProperty("allocationLedgerBalance")
  @NonNull
  private Amount allocationLedgerBalance;
}
