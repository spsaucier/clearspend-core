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
  EXPENSE_CATEGORY("expense_categories"),
  FILE_STORE("FileStore"),
  HOLD("Hold"),
  JOURNAL_ENTRY("JournalEntry"),
  LEDGER_ACCOUNT("LedgerAccount"),
  NETWORK_MESSAGE("NetworkMessage"),
  PENDING_STRIPE_TRANSFER("pending_stripe_transfer"),
  POSTING("Posting"),
  RECEIPT("Receipt"),
  SPEND_LIMIT("SpendLimit"),
  TRANSACTION_SYNC_LOG("transaction_sync_log"),
  USER("user"),
  USER_ALLOCATION_ROLE("user_allocation_role");

  private final String name;

  Table(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
