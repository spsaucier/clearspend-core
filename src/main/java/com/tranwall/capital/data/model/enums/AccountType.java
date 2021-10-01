package com.tranwall.capital.data.model.enums;

public enum AccountType {
  BUSINESS(LedgerAccountType.BUSINESS),
  ALLOCATION(LedgerAccountType.ALLOCATION),
  CARD(LedgerAccountType.CARD);

  private final LedgerAccountType ledgerAccountType;

  AccountType(LedgerAccountType ledgerAccountType) {
    this.ledgerAccountType = ledgerAccountType;
  }

  public LedgerAccountType getLedgerAccountType() {
    return ledgerAccountType;
  }
}
