package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Amount {

  @JsonProperty("currency")
  @NonNull
  @NotNull(message = "currency is null")
  @Schema(example = "USD")
  private Currency currency;

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount is null")
  @Schema(example = "100")
  private BigDecimal amount;

  public com.tranwall.capital.common.data.model.Amount toAmount() {
    return new com.tranwall.capital.common.data.model.Amount(currency, amount);
  }

  public static Amount of(com.tranwall.capital.common.data.model.Amount amount) {
    return new Amount(amount.getCurrency(), amount.getAmount());
  }
}
