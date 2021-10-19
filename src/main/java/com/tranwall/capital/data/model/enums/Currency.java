package com.tranwall.capital.data.model.enums;

public enum Currency {
  USD(2);

  private int decimalScale;

  Currency(int decimalScale) {
    this.decimalScale = decimalScale;
  }

  public int getDecimalScale() {
    return decimalScale;
  }
}
