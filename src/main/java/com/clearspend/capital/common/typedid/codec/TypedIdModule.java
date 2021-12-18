package com.clearspend.capital.common.typedid.codec;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TypedIdModule extends SimpleModule {

  private static final long serialVersionUID = -5886924783510114246L;

  public TypedIdModule() {
    super("TypedIdModule");
    addSerializer(new TypedIdSerializer());
    addDeserializer(TypedId.class, new TypedIdDeserializer());
  }
}
