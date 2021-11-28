package com.tranwall.capital.client.i2c.push.controller.type;

public enum CardStatus {
  ISSUED_INACTIVE("A", "Card has been issued but not yet activated"),
  OPEN("B", "All transactions allowed"),
  LOST_CARD("C", "Lost card"),
  STOLEN_CARD("D", "Stolen card"),
  NO_WITHDRAWALS("E", "No withdrawals are allowed to be made on card. (Restricted)"),
  CLOSED("F", "Card account has been closed"),
  LOST_NOT_CAPTURED("G", "Lost card not captured"),
  STOLEN_NOT_CAPTURED("H", "Stolen card not captured"),
  INACTIVE("I", "Card is inactive or blocked"),
  CARD_REISSUE("R", "Card reissue"),
  FRAUD_BLOCK("S", "Card blocked due to fraud"),
  ;

  private String i2cCardStatus;
  private String description;

  CardStatus(String i2cCardStatus, String description) {
    this.i2cCardStatus = i2cCardStatus;
    this.description = description;
  }

  public String getI2cCardStatus() {
    return i2cCardStatus;
  }
}
