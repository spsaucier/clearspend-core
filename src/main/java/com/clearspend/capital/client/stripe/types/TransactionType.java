package com.clearspend.capital.client.stripe.types;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {
  CAPTURE(StripeKey.CAPTURE),
  REFUND(StripeKey.REFUND),
  UNKNOWN("");

  private final String stripeKey;

  public static TransactionType fromStripeKey(String value) {
    return Optional.ofNullable(value)
        .map(
            v ->
                switch (v) {
                  case StripeKey.CAPTURE -> CAPTURE;
                  case StripeKey.REFUND -> REFUND;
                  default -> UNKNOWN;
                })
        .orElse(UNKNOWN);
  }

  private interface StripeKey {
    String CAPTURE = "capture";
    String REFUND = "refund";
  }
}
