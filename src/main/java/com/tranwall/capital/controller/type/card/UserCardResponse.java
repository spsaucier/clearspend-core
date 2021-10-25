package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UserCardResponse {

  @JsonProperty("card")
  @NonNull
  @NotNull(message = "card required")
  private Card card;

  @JsonProperty("ledgerBalance")
  @NonNull
  @NotNull(message = "ledgerBalance required")
  private Amount ledgerBalance;

  @JsonProperty("availableBalance")
  @NonNull
  @NotNull(message = "availableBalance required")
  private Amount availableBalance;

  @JsonProperty("allocationName")
  @NonNull
  @NotNull(message = "allocationName required")
  private String allocationName;
}
