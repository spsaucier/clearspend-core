package com.tranwall.capital.common.typedid.codec;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tranwall.capital.common.typedid.data.TypedId;

public class TypedIdModule extends SimpleModule {

  private static final long serialVersionUID = -5886924783510114246L;

  public TypedIdModule() {
    super("TypedIdModule");
    addSerializer(new TypedIdSerializer());
    addDeserializer(TypedId.class, new TypedIdDeserializer());
  }
}
