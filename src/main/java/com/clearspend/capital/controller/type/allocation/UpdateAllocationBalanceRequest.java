package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.controller.type.Amount;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateAllocationBalanceRequest {

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount is null")
  @Schema(example = "{currency=USD,amount=100}")
  private Amount amount;

  @JsonProperty("notes")
  @NonNull
  @NotNull(message = "notes is null")
  @Schema(example = "CSR credit")
  private String notes;
}
