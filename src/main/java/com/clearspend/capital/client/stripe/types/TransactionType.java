package com.clearspend.capital.client.stripe.types;

import java.util.Map;
import java.util.Optional;

public enum TransactionType {
  CAPTURE,
  REFUND,
  UNKNOWN;

  private static final Map<String, TransactionType> mapping =
      Map.of(
          "capture", CAPTURE,
          "refund", REFUND);

  public static TransactionType from(String value) {
    return Optional.ofNullable(value).map(v -> mapping.getOrDefault(v, UNKNOWN)).orElse(UNKNOWN);
  }
}
