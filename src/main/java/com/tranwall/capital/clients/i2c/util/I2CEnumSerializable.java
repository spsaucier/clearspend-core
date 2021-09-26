package com.tranwall.capital.clients.i2c.util;

public interface I2CEnumSerializable<T extends Enum<T>> {

    String serialize ();
}
