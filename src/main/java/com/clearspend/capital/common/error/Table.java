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
  CHART_OF_ACCOUNTS_MAPPING("codat_account_expense_category_mappings"),
  EXPENSE_CATEGORY("expense_categories"),
  FILE_STORE("FileStore"),
  HOLD("Hold"),
  JOURNAL_ENTRY("JournalEntry"),
  LEDGER_ACCOUNT("LedgerAccount"),
  NETWORK_MESSAGE("NetworkMessage"),
  PENDING_STRIPE_TRANSFER("pending_stripe_transfer"),
  PLAID_LOG_ENTRY("plaid_log_entry"),
  POSTING("Posting"),
  RECEIPT("Receipt"),
  SPEND_LIMIT("SpendLimit"),
  TRANSACTION_SYNC_LOG("transaction_sync_log"),
  USER("user"),
  USER_ALLOCATION_ROLE("user_allocation_role"),
  CHART_OF_ACCOUNTS("chart_of_accounts");

  private final String name;

  Table(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
