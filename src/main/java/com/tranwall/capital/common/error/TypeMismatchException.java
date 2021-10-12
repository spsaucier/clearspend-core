package com.tranwall.capital.common.error;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class TypeMismatchException extends RuntimeException {

  public <T extends Enum<T>> TypeMismatchException(Enum<T> expected, Enum<T> have) {
    super(
        String.format(
            "Invalid %s, expected %s but have %s", expected.getClass().getName(), expected, have));
  }
}
