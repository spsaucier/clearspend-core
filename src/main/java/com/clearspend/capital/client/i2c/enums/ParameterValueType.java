package com.clearspend.capital.client.i2c.enums;

import com.clearspend.capital.client.i2c.util.I2CEnumSerializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParameterValueType implements I2CEnumSerializable<ParameterValueType> {
  MIN_AND_MAX_FIELDS("M"),
  MIN_FIELD_ONLY("N"),
  VALUE_FIELD_ONLY("V"),
  MAX_FIELD_ONLY("X"),
  ALL_FIELDS("A"),
  ADS("B");

  private final String i2cParameterValueType;

  @Override
  public String serialize() {
    return i2cParameterValueType;
  }
}
