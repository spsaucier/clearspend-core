package com.clearspend.capital.data.model.enums;

public enum AuthorizationMethod {

  /** The card number was manually entered into a terminal. */
  KEYED_IN,

  /** The card was physically swiped in a terminal. */
  SWIPE,

  /**
   * The card was physically present and inserted into a chip-enabled terminal. The transaction is
   * cryptographically secured.
   */
  CHIP,

  /**
   * The card was tapped on a contactless-enabled terminal. If a digital wallet copy of the card was
   * used, the wallet field will be present.
   */
  CONTACTLESS,

  /**
   * The card was used in a card-not-present scenario, such as a transaction initiated at an online
   * e-commerce checkout.
   */
  ONLINE;

  public static AuthorizationMethod fromString(String value) {
    return AuthorizationMethod.valueOf(value.toUpperCase());
  }
}
