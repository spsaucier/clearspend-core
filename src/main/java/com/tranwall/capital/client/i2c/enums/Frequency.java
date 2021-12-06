package com.tranwall.capital.client.i2c.enums;

import com.tranwall.capital.client.i2c.util.I2CEnumSerializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Frequency implements I2CEnumSerializable<Frequency> {
  DAILY("D"),
  WEEKLY("W"),
  MONTHLY("M"),
  CUSTOM("C"),
  HOURLY("H"),
  BOUNDARY("B"),
  YEARLY("Y"),
  MINUTE("T"),
  SECOND("S"),
  LIFE_TIME("L"),
  STATEMENT_CYCLE("E"),
  TIME_RESTRICTION("Z");

  private final String i2cFrequency;

  @Override
  public String serialize() {
    return i2cFrequency;
  }
}
