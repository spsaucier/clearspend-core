package com.clearspend.capital.client.i2c.enums;

import com.clearspend.capital.client.i2c.util.I2CEnumSerializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertRecipient implements I2CEnumSerializable<AlertRecipient> {
  CARDHOLDER("C"),
  PRIMARY_CARDHOLDER("P"),
  ALL_CARDHOLDERS("B");

  private final String i2cAlertRecipient;

  @Override
  public String serialize() {
    return i2cAlertRecipient;
  }
}
