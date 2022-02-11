package com.clearspend.capital.data.model.enums;

import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PaymentType {
  POS(
      EnumSet.of(
          AuthorizationMethod.SWIPE, AuthorizationMethod.CHIP, AuthorizationMethod.CONTACTLESS)),
  ONLINE(EnumSet.of(AuthorizationMethod.ONLINE)),
  MANUAL_ENTRY(EnumSet.of(AuthorizationMethod.KEYED_IN));

  private final Set<AuthorizationMethod> authorizationMethods;

  public static PaymentType from(AuthorizationMethod authorizationMethod) {
    return EnumSet.allOf(PaymentType.class).stream()
        .filter(paymentMethod -> paymentMethod.authorizationMethods.contains(authorizationMethod))
        .findFirst()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Failed to get payment method for authorization method: %s"
                        .formatted(authorizationMethod)));
  }
}
