package com.clearspend.capital.testutils;

@FunctionalInterface
public interface ThrowingBiConsumer<T, R> {
  default void accept(final T input1, final R input2) {
    try {
      acceptThrows(input1, input2);
    } catch (final RuntimeException | AssertionError ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
  }

  void acceptThrows(final T input1, final R input2) throws Throwable;
}
