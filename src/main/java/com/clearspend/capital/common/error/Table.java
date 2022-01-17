package com.clearspend.capital.common.error;

public enum Table {
  ACCOUNT("Account"),
  ACCOUNT_ACTIVITY("AccountActivity"),
  ADJUSTMENT("Adjustment"),
  ALLOCATION("Allocation"),
  BUSINESS("Business"),
  BUSINESS_BANK_ACCOUNT("BusinessBankAccount"),
  BUSINESS_LIMIT("BusinessLimit"),
  BUSINESS_OWNER("BusinessOwner"),
  BUSINESS_PROSPECT("BusinessProspect"),
  CARD("Card"),
  HOLD("Hold"),
  LEDGER_ACCOUNT("LedgerAccount"),
  NETWORK_MESSAGE("NetworkMessage"),
  RECEIPT("Receipt"),
  SPEND_LIMIT("SpendLimit"),
  USER("user");

  private final String name;

  Table(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
