package com.clearspend.capital.common.error;

import com.clearspend.capital.data.model.enums.Currency;

public class CurrencyMismatchException extends RuntimeException {

  public CurrencyMismatchException(Currency expected, Currency have) {
    super(String.format("Currency mismatch, expected %s but have %s", expected, have));
  }
}
