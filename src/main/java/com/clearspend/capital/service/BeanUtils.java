package com.clearspend.capital.service;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@UtilityClass
public class BeanUtils {

  public <T> void setNotNull(T value, Consumer<T> consumer) {
    if (value != null) {
      consumer.accept(value);
    }
  }

  public <T, V> T getOrDefault(V object, Function<V, T> extractor, T defaultValue) {
    return Optional.ofNullable(object).map(extractor).orElse(defaultValue);
  }

  public <T> void setIfTrue(java.lang.Boolean value, Consumer<java.lang.Boolean> consumer) {
    if (BooleanUtils.isTrue(value)) {
      consumer.accept(value);
    }
  }

  public void setNotEmpty(String value, Consumer<String> consumer) {
    if (StringUtils.isNotEmpty(value)) {
      consumer.accept(value);
    }
  }

  public <T> void setNotEmpty(
      final Collection<T> collection, final Consumer<Collection<T>> consumer) {
    if (collection != null && !CollectionUtils.isEmpty(collection)) {
      consumer.accept(collection);
    }
  }
}
