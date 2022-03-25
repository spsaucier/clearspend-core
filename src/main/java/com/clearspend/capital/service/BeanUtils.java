package com.clearspend.capital.service;

import java.util.Collection;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@UtilityClass
public class BeanUtils {

  public <T> void setNotNull(T value, Consumer<T> consumer) {
    if (value != null) {
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
