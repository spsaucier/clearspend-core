package com.clearspend.capital.service;

import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

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
}
