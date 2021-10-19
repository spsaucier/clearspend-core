package com.tranwall.capital.common.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.error.CurrencyMismatchException;
import com.tranwall.capital.common.utils.BigDecimalUtils;
import com.tranwall.capital.data.model.enums.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@AllArgsConstructor(onConstructor = @__({@JsonCreator}))
@MappedSuperclass
public class Amount {

  @Enumerated(value = EnumType.STRING)
  @JsonProperty("currency")
  private Currency currency;

  @JsonProperty("amount")
  private BigDecimal amount;

  public static Amount of(Currency currency, BigDecimal amount) {
    return new Amount(
        currency, amount.setScale(currency.getDecimalScale(), RoundingMode.UNNECESSARY));
  }

  public static Amount add(Amount amount, Amount other) {
    if (amount.getCurrency() != other.getCurrency()) {
      throw new CurrencyMismatchException(amount.getCurrency(), other.getCurrency());
    }

    return of(amount.currency, amount.getAmount().add(other.getAmount()));
  }

  public static Amount sub(Amount amount, Amount other) {
    if (amount.getCurrency() != other.getCurrency()) {
      throw new CurrencyMismatchException(amount.getCurrency(), other.getCurrency());
    }

    return of(amount.currency, amount.getAmount().subtract(other.getAmount()));
  }

  public static Amount negate(Amount amount) {
    return of(amount.currency, amount.getAmount().negate());
  }

  public boolean isSmallerThan(@NonNull Amount that) {
    return BigDecimalUtils.isSmallerThan(this.amount, that.amount);
  }

  public boolean isNegative() {
    return BigDecimalUtils.isNegative(amount);
  }

  public boolean isPositive() {
    return BigDecimalUtils.isPositive(amount);
  }
}
