package com.clearspend.capital.util.function;

import java.util.function.Function;
import java.util.function.Supplier;

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

  static <O> Supplier<O> sneakyThrows(final ThrowingSupplier<O> fn) {
    return () -> {
      try {
        return fn.get();
      } catch (Throwable ex) {
        if (ex instanceof RuntimeException runEx) {
          throw runEx;
        }
        throw new RuntimeException(ex);
      }
    };
  }

  static void sneakyThrows(ThrowingRunnable fn) {
    try {
      fn.run();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @FunctionalInterface
  interface ThrowingFunction<I, O> {
    O apply(final I input) throws Exception;
  }

  @FunctionalInterface
  interface ThrowingSupplier<O> {
    O get() throws Exception;
  }

  @FunctionalInterface
  interface ThrowingRunnable {
    void run() throws Exception;
  }
}
