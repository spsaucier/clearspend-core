package com.tranwall.common.masking;

import com.tranwall.common.masking.annotation.Sensitive;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

// only addresses serialization, we are not worried about deserializing at this stage
public class MaskAnnotationIntrospector extends NopAnnotationIntrospector {

  @Override
  public Object findSerializer(Annotated annotated) {
    if (annotated.hasAnnotation(Sensitive.class)) {
      return SensitiveFieldSerializer.class;
    }

    return null;
  }
}
