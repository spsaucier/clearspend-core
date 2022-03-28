package com.clearspend.capital.common.data.util;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

@AllArgsConstructor
public class MustacheSqlBeanPropertyContext extends AbstractMap<String, Object> {
  private final BeanPropertySqlParameterSource source;

  @Override
  public Set<String> keySet() {
    return Arrays.stream(source.getParameterNames()).collect(Collectors.toSet());
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return Arrays.stream(source.getParameterNames())
        .map(name -> new SimpleEntry<>(name, source.getValue(name)))
        .collect(Collectors.toSet());
  }

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

  @Override
  public int size() {
    return source.getParameterNames().length;
  }

  @Override
  public boolean isEmpty() {
    return source.getParameterNames().length == 0;
  }
}
