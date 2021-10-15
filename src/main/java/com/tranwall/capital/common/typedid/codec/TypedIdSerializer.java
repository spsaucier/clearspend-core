package com.tranwall.capital.common.typedid.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.io.IOException;

public class TypedIdSerializer extends StdSerializer<TypedId<?>> {

  private static final long serialVersionUID = 664822887447479718L;

  private final UUIDSerializer delegate;

  public TypedIdSerializer() {
    super(TypedId.class, false);
    this.delegate = new UUIDSerializer();
  }

  @Override
  public void serialize(TypedId<?> value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    delegate.serialize(value.toUuid(), gen, serializers);
  }
}
