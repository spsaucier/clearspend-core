package com.clearspend.capital.data.model.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Currency {
  UNSPECIFIED(2),
  USD(2);

  private int decimalScale;

  Currency(int decimalScale) {
    this.decimalScale = decimalScale;
  }

  public int getDecimalScale() {
    return decimalScale;
  }

  public static Currency of(String currency) {
    try {
      return currency == null ? UNSPECIFIED : valueOf(currency.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Invalid currency detected: " + currency);
      return UNSPECIFIED;
    }
  }
}
