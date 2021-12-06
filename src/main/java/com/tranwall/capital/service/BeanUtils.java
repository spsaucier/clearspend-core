package com.tranwall.capital.service;

import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class BeanUtils {

  <T> void setNotNull(T value, Consumer<T> consumer) {
    if (value != null) {
      consumer.accept(value);
    }
  }

  void setNotEmpty(String value, Consumer<String> consumer) {
    if (StringUtils.isNotEmpty(value)) {
      consumer.accept(value);
    }
  }
}
