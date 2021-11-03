package com.tranwall.capital.controller.type.business.bankaccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TransactBankAccountRequest {

  @JsonProperty("bankAccountTransactType")
  @NonNull
  private FundsTransactType bankAccountTransactType;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;

  @JsonProperty("isOnboarding")
  @Schema(
      title = "Indicate if transaction is requested during the onboarding process",
      example = "false")
  private boolean isOnboarding;
}
