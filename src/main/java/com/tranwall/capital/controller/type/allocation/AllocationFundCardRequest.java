package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AllocationFundCardRequest {

  @JsonProperty("allocationAccountId")
  @NonNull
  private TypedId<AccountId> allocationAccountId;

  @JsonProperty("cardId")
  @NonNull
  private TypedId<CardId> cardId;

  @JsonProperty("bankAccountTransactType")
  @NonNull
  private FundsTransactType fundsTransactType;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;
}
