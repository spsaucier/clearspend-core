package com.clearspend.capital.client.i2c.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.function.Function;

public abstract class I2CEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String text = p.getText();
    if (text != null && text.length() > 0) {
      return getDeserializerFunction().apply(text);
    }

    return null;
  }

  protected abstract Function<String, T> getDeserializerFunction();
}
