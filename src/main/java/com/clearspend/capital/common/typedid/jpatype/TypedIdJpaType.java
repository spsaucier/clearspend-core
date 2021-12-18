package com.clearspend.capital.common.typedid.jpatype;

import com.clearspend.capital.common.typedid.data.TypedId;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.PostgresUUIDType.PostgresUUIDSqlTypeDescriptor;

@SuppressWarnings("rawtypes")
public class TypedIdJpaType extends AbstractSingleColumnStandardBasicType<TypedId> {

  public TypedIdJpaType() {
    super(PostgresUUIDSqlTypeDescriptor.INSTANCE, TypedIdJavaDescriptor.INSTANCE);
  }

  @Override
  public String getName() {
    return TypedId.class.getSimpleName();
  }

  @Override
  protected boolean registerUnderJavaType() {
    return true;
  }
}
