package com.clearspend.capital.client.stripe.types;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum StripeNetwork {
  CARD,
  STRIPE,
  ACH,
  US_DOMESTIC_WIRE,
  UNKNOWN;

  private static final Map<String, StripeNetwork> mapping =
      Stream.of(StripeNetwork.values())
          .collect(Collectors.toMap(sn -> sn.name().toLowerCase(), sn -> sn));

  public static StripeNetwork from(String value) {
    return mapping.getOrDefault(value, UNKNOWN);
  }
}
