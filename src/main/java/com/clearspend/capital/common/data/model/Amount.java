package com.clearspend.capital.common.data.model;

import com.clearspend.capital.common.error.AmountException;
import com.clearspend.capital.common.error.AmountException.AmountType;
import com.clearspend.capital.common.error.CurrencyMismatchException;
import com.clearspend.capital.data.model.enums.Currency;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  public static Amount fromStripeAmount(Currency currency, Long amount) {
    return switch (currency) {
      case UNSPECIFIED -> null;
      case USD -> of(
          currency,
          BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY));
    };
  }

  public static Amount min(Amount left, Amount right) {
    assert left.currency.equals(right.currency) : "currency mismatch";
    return left.isLessThan(right) ? left : right;
  }

  public long toStripeAmount() {
    return switch (currency) {
      case UNSPECIFIED -> 0;
      case USD -> amount.multiply(BigDecimal.valueOf(100)).longValue();
    };
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

  public boolean isLessThan(@NonNull Amount that) {
    return amount.compareTo(that.amount) < 0;
  }

  public boolean isGreaterThan(@NonNull Amount that) {
    return amount.compareTo(that.amount) > 0;
  }

  @JsonIgnore
  public boolean isGreaterThanZero() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  @JsonIgnore
  public boolean isGreaterThanOrEqualZero() {
    return amount.compareTo(BigDecimal.ZERO) >= 0;
  }

  @JsonIgnore
  public boolean isLessThanOrEqualToZero() {
    return amount.compareTo(BigDecimal.ZERO) <= 0;
  }

  @JsonIgnore
  public boolean isLessThanZero() {
    return amount.compareTo(BigDecimal.ZERO) < 0;
  }

  @JsonIgnore
  public boolean isNegative() {
    return amount.compareTo(BigDecimal.ZERO) < 0;
  }

  @JsonIgnore
  public boolean isPositive() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  public void ensureNegative() {
    if (!isNegative()) {
      throw new AmountException(AmountType.NEGATIVE, this);
    }
  }

  public void ensureNonNegative() {
    if (isNegative()) {
      throw new AmountException(AmountType.POSITIVE, this);
    }
  }
}
