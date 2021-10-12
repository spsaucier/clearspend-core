package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AllocationFundCardRequest {

  @JsonProperty("allocationAccountId")
  @NonNull
  private UUID allocationAccountId;

  @JsonProperty("cardId")
  @NonNull
  private UUID cardId;

  @JsonProperty("bankAccountTransactType")
  @NonNull
  private FundsTransactType fundsTransactType;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;
}
