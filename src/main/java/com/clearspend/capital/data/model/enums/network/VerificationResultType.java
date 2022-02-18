package com.clearspend.capital.data.model.enums.network;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum VerificationResultType {
  UNKNOWN(""),
  NOT_PROVIDED("not_provided"),
  MISMATCH("mismatch"),
  MATCH("match");

  private String stripeVerificationResultType;

  private static final Map<String, VerificationResultType> stripeVerificationResultMap =
      initializeMap();

  private static Map<String, VerificationResultType> initializeMap() {
    return Arrays.stream(VerificationResultType.values())
        .collect(
            Collectors.toUnmodifiableMap(e -> e.stripeVerificationResultType, Function.identity()));
  }

  VerificationResultType(String stripeVerificationResultType) {
    this.stripeVerificationResultType = stripeVerificationResultType;
  }

  public static VerificationResultType fromStripe(String stripeVerificationResult) {
    VerificationResultType eventType = stripeVerificationResultMap.get(stripeVerificationResult);
    return eventType != null ? eventType : UNKNOWN;
  }
}
