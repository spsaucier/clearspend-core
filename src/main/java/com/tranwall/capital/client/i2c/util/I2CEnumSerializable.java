package com.tranwall.capital.client.i2c.util;

public interface I2CEnumSerializable<T extends Enum<T>> {

  String serialize();
}
