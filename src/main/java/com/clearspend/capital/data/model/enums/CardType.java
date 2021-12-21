package com.clearspend.capital.data.model.enums;

public enum CardType {
  PLASTIC,
  VIRTUAL,
  ;

  public String toStripeType() {
    return switch (this) {
      case PLASTIC -> "physical";
      case VIRTUAL -> "virtual";
    };
  }
}
