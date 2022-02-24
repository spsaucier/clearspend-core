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
  FILE_STORE("FileStore"),
  HOLD("Hold"),
  LEDGER_ACCOUNT("LedgerAccount"),
  NETWORK_MESSAGE("NetworkMessage"),
  RECEIPT("Receipt"),
  SPEND_LIMIT("SpendLimit"),
  USER("user"),
  USER_ALLOCATION_ROLE("user_allocation_role"),
  EXPENSE_CATEGORY("expense_categories"),
  PENDING_STRIPE_TRANSFER("pending_stripe_transfer"),
  TRANSACTION_SYNC_LOG("transaction_sync_log");

  private final String name;

  Table(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
