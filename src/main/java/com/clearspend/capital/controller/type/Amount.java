package com.clearspend.capital.controller.type;

import com.clearspend.capital.data.model.enums.Currency;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  public com.clearspend.capital.common.data.model.Amount toAmount() {
    return new com.clearspend.capital.common.data.model.Amount(currency, amount);
  }

  public static Amount of(com.clearspend.capital.common.data.model.Amount amount) {
    return amount != null ? new Amount(amount.getCurrency(), amount.getAmount()) : null;
  }

  public static com.clearspend.capital.common.data.model.Amount toDomainAmount(Amount amount) {
    return amount != null ? amount.toAmount() : null;
  }
}
