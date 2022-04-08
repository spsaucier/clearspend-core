package com.clearspend.capital.util.function;

import java.util.function.Function;

public interface ThrowableFunctions {
  static <I, O> Function<I, O> sneakyThrows(final ThrowingFunction<I, O> fn) {
    return input -> {
      try {
        return fn.apply(input);
      } catch (Throwable ex) {
        if (ex instanceof RuntimeException runEX) {
          throw runEX;
        }
        throw new RuntimeException(ex);
      }
    };
  }

  @FunctionalInterface
  interface ThrowingFunction<I, O> {
    O apply(final I input) throws Exception;
  }
}
