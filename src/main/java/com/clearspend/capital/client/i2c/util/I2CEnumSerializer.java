package com.clearspend.capital.client.i2c.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class I2CEnumSerializer extends JsonSerializer<I2CEnumSerializable<?>> {

  @Override
  public void serialize(
      I2CEnumSerializable<?> value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeString(value.serialize());
  }
}
