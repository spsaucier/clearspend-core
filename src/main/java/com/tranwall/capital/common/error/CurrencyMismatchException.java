package com.tranwall.capital.common.error;

import com.tranwall.capital.data.model.enums.Currency;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class CurrencyMismatchException extends RuntimeException {
  public CurrencyMismatchException(Currency expected, Currency have) {
    super(String.format("Currency mismatch, expected %s but have %s", expected, have));
  }
}
