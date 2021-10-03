package com.tranwall.capital.client.i2c.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class I2CBooleanDeserializer extends JsonDeserializer<Boolean> {

  private static final String YES = "Y";

  @Override
  public Boolean deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    return YES.equals(parser.getText());
  }
}
