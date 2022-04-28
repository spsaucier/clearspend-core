package com.clearspend.capital.util;

import com.google.common.base.Splitter;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/**
 * Convenience class for getting at contents of a Yaml file without having to write lots of code to
 * traverse a tree. Use the "get" method with dot-delimited strings to indicate the desired info and
 * cast it to the right type at the end.
 *
 * <p>If the result of a call to {{@link #get(String)}} is a Map, it will be rendered as a
 * SimpleYaml.
 */
public class SimpleYaml extends AbstractMap<String, Object> {

  private final Map<String, Object> data;

  @SuppressWarnings("unchecked")
  public SimpleYaml(InputStream stream) {
    this((Map<String, Object>) new Yaml().load(stream));
  }

  private SimpleYaml(Map<String, Object> map) {
    this.data = map;
  }

  @SuppressWarnings("unchecked")
  public Object get(String property) {
    Object result = data;
    for (String term : Splitter.on('.').split(property)) {
      result = ((Map<String, Object>) result).get(term);
    }
    if (result instanceof Map) {
      result = new SimpleYaml((Map<String, Object>) result);
    }
    return result;
  }

  @NotNull
  @Override
  public Set<Entry<String, Object>> entrySet() {
    return data.entrySet();
  }

  @Override
  public boolean containsKey(Object key) {
    return data.containsKey(key);
  }

  @Override
  public int size() {
    return data.size();
  }
}
