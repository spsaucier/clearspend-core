package com.clearspend.capital.controller.type.business.accounting;

import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateBusinessAccountingStepRequest {
  @JsonProperty("accountingSetupStep")
  @NonNull
  private AccountingSetupStep accountingSetupStep;
}
