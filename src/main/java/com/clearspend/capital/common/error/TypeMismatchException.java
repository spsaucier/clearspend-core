package com.clearspend.capital.common.error;

public class TypeMismatchException extends RuntimeException {

  public <T extends Enum<T>> TypeMismatchException(Enum<T> expected, Enum<T> have) {
    super(
        String.format(
            "Invalid %s, expected %s but have %s", expected.getClass().getName(), expected, have));
  }
}
