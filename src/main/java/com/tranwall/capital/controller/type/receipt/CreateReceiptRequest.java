package com.tranwall.capital.controller.type.receipt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CreateReceiptRequest {

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount is null")
  private Amount amount;
}
