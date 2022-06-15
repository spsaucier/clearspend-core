package com.clearspend.capital.util.function;

import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * Crude implementation of a functional pipeline. Each function argument is called in sequence, with
 * the result of the preceding function passed as an argument to the succeeding function. All
 * functions must be of arity-1.
 */
public interface Pipeline {
  static <A, B> B pipe(@NonNull final Supplier<A> fn1, @NonNull final Function<A, B> fn2) {
    final A result1 = fn1.get();
    return fn2.apply(result1);
  }

  static <A, B, C> C pipe(
      @NonNull final Supplier<A> fn1,
      @NonNull final Function<A, B> fn2,
      @NonNull final Function<B, C> fn3) {
    final A result1 = fn1.get();
    final B result2 = fn2.apply(result1);
    return fn3.apply(result2);
  }
}
