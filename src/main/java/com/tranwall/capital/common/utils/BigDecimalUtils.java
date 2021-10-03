package com.tranwall.capital.common.utils;

import java.math.BigDecimal;
import lombok.NonNull;

public class BigDecimalUtils {

  public static boolean isPositive(@NonNull BigDecimal value) {
    return value.compareTo(BigDecimal.ZERO) > 0;
  }

  public static boolean isZero(@NonNull BigDecimal value) {
    return value.compareTo(BigDecimal.ZERO) == 0;
  }

  public static boolean isNegative(@NonNull BigDecimal value) {
    return value.compareTo(BigDecimal.ZERO) < 0;
  }

  public static boolean isLargerThanOrEqualTo(
      @NonNull BigDecimal first, @NonNull BigDecimal second) {
    return first.compareTo(second) >= 0;
  }

  public static boolean isSmallerThanOrEqualTo(
      @NonNull BigDecimal first, @NonNull BigDecimal second) {
    return first.compareTo(second) <= 0;
  }

  public static boolean isSmallerThan(
      @NonNull BigDecimal first, @NonNull BigDecimal second) {
    return first.compareTo(second) < 0;
  }

  public static boolean isEqualTo(@NonNull BigDecimal first, @NonNull BigDecimal second) {
    return first.compareTo(second) == 0;
  }

  public static boolean isEqualToIgnoringSign(
      @NonNull BigDecimal first, @NonNull BigDecimal second) {
    return isEqualTo(first.abs(), second.abs());
  }

  public static boolean isNotEqualToIgnoringSign(
      @NonNull BigDecimal first, @NonNull BigDecimal second) {
    return !isEqualToIgnoringSign(first, second);
  }

  public static BigDecimal toPositive(@NonNull BigDecimal value) {
    return value.abs();
  }

  public static BigDecimal toNegative(@NonNull BigDecimal value) {
    return value.abs().negate();
  }
}
