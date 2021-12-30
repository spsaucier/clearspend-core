package com.clearspend.capital.data.model.enums.card;

public enum CardType {
  PHYSICAL,
  VIRTUAL,
  ;

  public String toStripeType() {
    return switch (this) {
      case PHYSICAL -> "physical";
      case VIRTUAL -> "virtual";
    };
  }
}
