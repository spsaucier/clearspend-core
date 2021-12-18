package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AllocationFundCardRequest {

  @JsonProperty("allocationAccountId")
  @NonNull
  @NotNull(message = "allocation account id is null")
  @Schema(example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<AccountId> allocationAccountId;

  @JsonProperty("cardId")
  @NonNull
  @NotNull(message = "cardId is null")
  @Schema(example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<CardId> cardId;

  @JsonProperty("reallocationType")
  @NonNull
  @NotNull(message = "reallocationType is null")
  @Schema(example = "DEPOSIT")
  private AllocationReallocationType reallocationType;

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount is null")
  @Schema(example = "{currency=USD,amount=100}")
  private Amount amount;
}
