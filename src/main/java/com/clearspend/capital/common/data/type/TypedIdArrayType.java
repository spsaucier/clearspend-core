package com.clearspend.capital.common.data.type;

import com.clearspend.capital.common.typedid.data.TypedId;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.springframework.util.ObjectUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TypedIdArrayType implements UserType {

  @Override
  public int[] sqlTypes() {
    return new int[] {Types.ARRAY};
  }

  @Override
  public Class returnedClass() {
    return List.class;
  }

  @Override
  public boolean equals(Object x, Object y) throws HibernateException {
    return ObjectUtils.nullSafeEquals(x, y);
  }

  @Override
  public int hashCode(Object x) throws HibernateException {
    return ObjectUtils.nullSafeHashCode(x);
  }

  @Override
  public Object nullSafeGet(
      ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
      throws HibernateException, SQLException {
    if (names != null && names[0] != null && rs != null) {
      java.sql.Array values = rs.getArray(names[0]);
      if (values != null) {
        return Arrays.stream((UUID[]) values.getArray())
            .map(TypedId::new)
            .collect(Collectors.toList());
      }
    }
    return new ArrayList();
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
      throws HibernateException, SQLException {
    if (value != null) {
      String[] values =
          ((List<TypedId>) value).stream().map(TypedId::toString).toArray(String[]::new);
      st.setArray(index, session.connection().createArrayOf("uuid", values));
    } else {
      st.setArray(index, session.connection().createArrayOf("uuid", new String[0]));
    }
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    if (value != null) {
      return new ArrayList<TypedId>((List) value);
    }
    return new ArrayList();
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Serializable disassemble(Object value) throws HibernateException {
    return (Serializable) value;
  }

  @Override
  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return cached;
  }

  @Override
  public Object replace(Object original, Object target, Object owner) throws HibernateException {
    return original;
  }
}
