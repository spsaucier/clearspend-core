package com.clearspend.capital.util.function;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Contains throwing versions of common functions.
 *
 * <p>sneakyThrows() static method is on the functional interfaces to avoid type signature clashes.
 */
public interface ThrowableFunctions {
  @FunctionalInterface
  interface ThrowingFunction<I, O> {
    O apply(final I input) throws Exception;

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
  }

  @FunctionalInterface
  interface ThrowingSupplier<O> {
    O get() throws Exception;

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
  }

  interface ThrowingConsumer<I> {
    void accept(final I value) throws Exception;

    static <I> Consumer<I> sneakyThrows(final ThrowingConsumer<I> fn) {
      return value -> {
        try {
          fn.accept(value);
        } catch (Throwable ex) {
          if (ex instanceof RuntimeException runEx) {
            throw runEx;
          }
          throw new RuntimeException(ex);
        }
      };
    }
  }

  @FunctionalInterface
  interface ThrowingRunnable {
    void run() throws Exception;

    static void sneakyThrows(final ThrowingRunnable fn) {
      try {
        fn.run();
      } catch (RuntimeException e) {
        throw e;
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
  }
}
