package com.clearspend.capital.controller.type.business.bankaccount;

import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TransactBankAccountRequest {

  @JsonProperty("bankAccountTransactType")
  @NonNull
  private BankAccountTransactType bankAccountTransactType;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;

  @JsonProperty("isOnboarding")
  @Schema(
      title = "Indicate if transaction is requested during the onboarding process",
      example = "false")
  private boolean isOnboarding;
}
