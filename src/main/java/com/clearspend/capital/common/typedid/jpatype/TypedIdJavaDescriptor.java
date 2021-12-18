package com.clearspend.capital.common.typedid.jpatype;

import com.clearspend.capital.common.typedid.data.TypedId;
import java.io.Serial;
import java.util.UUID;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

@SuppressWarnings({"rawtypes"})
public class TypedIdJavaDescriptor extends AbstractTypeDescriptor<TypedId> {

  @Serial private static final long serialVersionUID = -7694632690303044900L;

  public static final TypedIdJavaDescriptor INSTANCE = new TypedIdJavaDescriptor();

  protected TypedIdJavaDescriptor() {
    super(TypedId.class);
  }

  @Override
  public <X> X unwrap(TypedId value, Class<X> type, WrapperOptions options) {
    return UUIDTypeDescriptor.INSTANCE.unwrap(value.toUuid(), type, options);
  }

  @Override
  public <X> TypedId wrap(X value, WrapperOptions options) {
    if (value == null) {
      return null;
    }

    if (value instanceof byte[]) {
      return new TypedId(UUIDTypeDescriptor.ToBytesTransformer.INSTANCE.parse(value));
    }

    if (value instanceof String) {
      return new TypedId(UUIDTypeDescriptor.ToStringTransformer.INSTANCE.parse(value));
    }

    if (value instanceof UUID) {
      return new TypedId((UUID) value);
    }

    throw unknownWrap(value.getClass());
  }

  @Override
  public TypedId fromString(String string) {
    return new TypedId(string);
  }
}
