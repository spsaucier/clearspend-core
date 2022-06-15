package com.clearspend.capital.util.function;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;

public interface OptionalFunctions {
  /** Helpful for evaluating Optionals in switch expressions */
  enum OptionalState {
    PRESENT,
    EMPTY;
  }

  static OptionalState getOptionalState(@NonNull final Optional<?> optional) {
    if (optional.isPresent()) {
      return OptionalState.PRESENT;
    }
    return OptionalState.EMPTY;
  }

  static <A, B> B fold(
      @NonNull final Optional<A> optional,
      final Function<A, B> ifPresent,
      final Supplier<B> ifEmpty) {
    if (optional.isPresent()) {
      return ifPresent.apply(optional.get());
    }
    return ifEmpty.get();
  }

  /**
   * Unwrap the List of Optionals. Returns an Optional of a List of values if all Optionals in the
   * list have a value. If any of the Optionals are empty, returns an empty Optional.
   */
  static <T> Optional<List<T>> sequenceAll(final List<Optional<T>> optionals) {
    return sequenceAll(optionals.stream(), optionals.size());
  }

  static <T> Optional<List<T>> sequenceAll(final Optional<T>... optionals) {
    return Pipeline.pipe(
        () -> Arrays.stream(optionals),
        stream -> OptionalFunctions.sequenceAll(stream, optionals.length));
  }

  static <T> Optional<List<T>> sequenceAll(final T... nullableValues) {
    return Pipeline.pipe(
        () -> wrapNullableValues(nullableValues),
        stream -> OptionalFunctions.sequenceAll(stream, nullableValues.length));
  }

  private static <T> Optional<List<T>> sequenceAll(
      final Stream<Optional<T>> optionals, final int originalSize) {
    final List<T> values = getOptionalValues(optionals);
    if (values.size() == originalSize) {
      return Optional.of(values);
    }
    return Optional.empty();
  }

  /**
   * Unwrap the List of Optionals. If any Optionals have values, it returns an Optional-wrapped List
   * of those values. If no Optionals have values, it returns an empty Optional.
   */
  static <T> Optional<List<T>> sequenceAny(final List<Optional<T>> optionals) {
    return sequenceAny(optionals.stream());
  }

  static <T> Optional<List<T>> sequenceAny(final Optional<T>... optionals) {
    return Pipeline.pipe(() -> Arrays.stream(optionals), OptionalFunctions::sequenceAny);
  }

  static <T> Optional<List<T>> sequenceAny(final T... nullableValues) {
    return Pipeline.pipe(() -> wrapNullableValues(nullableValues), OptionalFunctions::sequenceAny);
  }

  private static <T> Optional<List<T>> sequenceAny(final Stream<Optional<T>> optionals) {
    final List<T> values = getOptionalValues(optionals);
    if (!values.isEmpty()) {
      return Optional.of(values);
    }
    return Optional.empty();
  }

  private static <T> List<T> getOptionalValues(final Stream<Optional<T>> optionals) {
    return optionals.filter(Optional::isPresent).map(Optional::get).toList();
  }

  private static <T> Stream<Optional<T>> wrapNullableValues(final T[] nullableValues) {
    return Arrays.stream(nullableValues).map(Optional::ofNullable);
  }
}
