package com.tranwall.capital.common.typedid.codec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.io.IOException;

public class TypedIdDeserializer extends StdDeserializer<TypedId<?>> {

  private static final long serialVersionUID = 7052820034000133559L;
  private final UUIDDeserializer delegate;

  public TypedIdDeserializer() {
    super(TypedId.class);
    this.delegate = new UUIDDeserializer();
  }

  @Override
  public TypedId<?> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    return new TypedId<>(delegate.deserialize(p, ctxt));
  }
}
