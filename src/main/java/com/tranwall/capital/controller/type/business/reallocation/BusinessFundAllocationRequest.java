package com.tranwall.capital.controller.type.business.reallocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BusinessFundAllocationRequest {

  @JsonProperty("allocationId")
  @NonNull
  private UUID allocationId;

  @JsonProperty("accountId")
  @NonNull
  private UUID accountId;

  @JsonProperty("fundsTransactType")
  @NonNull
  private FundsTransactType fundsTransactType;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;
}
