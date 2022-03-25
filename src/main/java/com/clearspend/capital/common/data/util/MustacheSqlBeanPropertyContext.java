package com.clearspend.capital.common.data.util;

import java.util.HashMap;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

@AllArgsConstructor
public class MustacheSqlBeanPropertyContext extends HashMap<String, Object> {
  private final BeanPropertySqlParameterSource source;

  @Override
  public boolean containsKey(Object key) {
    return source.hasValue(String.valueOf(key));
  }

  @Override
  public Object get(Object key) {
    try {
      return source.getValue(String.valueOf(key));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
