package com.tranwall.capital.client.i2c.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class I2CBooleanSerializer extends JsonSerializer<Boolean> {

  @Override
  public void serialize(Boolean value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeString(value ? "1" : "0");
  }
}
