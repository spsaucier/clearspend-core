package com.clearspend.capital.util.function;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface MapFunctions {
  static <K, V> Map<K, V> append(final Map<K, V> base, final K key, final V value) {
    final Map<K, V> newMap = new HashMap<>(base);
    newMap.put(key, value);
    return Collections.unmodifiableMap(newMap);
  }

  static <K, V> Map<K, V> union(final Map<K, V> map1, final Map<K, V> map2) {
    final Map<K, V> newMap = new HashMap<>(map1);
    newMap.putAll(map2);
    return Collections.unmodifiableMap(newMap);
  }
}
