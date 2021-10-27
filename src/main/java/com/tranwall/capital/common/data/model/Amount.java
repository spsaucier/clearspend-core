package com.tranwall.capital.common.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.error.AmountException;
import com.tranwall.capital.common.error.AmountException.AmountType;
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

  public static Amount of(Currency currency) {
    return new Amount(currency, BigDecimal.ZERO);
  }

  public Amount add(Amount amount) {
    if (currency != amount.getCurrency()) {
      throw new CurrencyMismatchException(currency, amount.getCurrency());
    }

    return of(currency, this.amount.add(amount.getAmount()));
  }

  public Amount sub(Amount amount) {
    if (currency != amount.getCurrency()) {
      throw new CurrencyMismatchException(currency, amount.getCurrency());
    }

    return of(currency, this.amount.subtract(amount.getAmount()));
  }

  public Amount abs() {
    return of(currency, amount.abs());
  }

  public Amount negate() {
    return of(currency, amount.negate());
  }

  public boolean isSmallerThan(@NonNull Amount that) {
    return BigDecimalUtils.isSmallerThan(amount, that.amount);
  }

  public boolean isLargerThan(@NonNull Amount that) {
    return BigDecimalUtils.isLargerThan(amount, that.amount);
  }

  public boolean isNegative() {
    return BigDecimalUtils.isNegative(amount);
  }

  public boolean isPositive() {
    return BigDecimalUtils.isPositive(amount);
  }

  public void ensurePositive() {
    if (isNegative()) {
      throw new AmountException(AmountType.POSITIVE, this);
    }
  }
}
