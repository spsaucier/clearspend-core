package com.clearspend.capital.data.model.enums;

public enum AccountActivityType {
  BANK_DEPOSIT_STRIPE, // stripe initiated ach transfer
  BANK_DEPOSIT_ACH, // externally initiated ach transfer
  BANK_DEPOSIT_WIRE, // externally initiated wire transfer
  BANK_DEPOSIT_RETURN,
  BANK_WITHDRAWAL,
  BANK_WITHDRAWAL_RETURN,
  MANUAL,
  NETWORK_AUTHORIZATION,
  NETWORK_CAPTURE,
  NETWORK_REFUND,
  REALLOCATE,
  FEE,
  CARD_FUND_RETURN; // unsolicited card credit (where the original auth is not referenced)

  public static AccountActivityType from(BankAccountTransactType bankAccountTransactType) {
    return bankAccountTransactType == BankAccountTransactType.DEPOSIT
        ? AccountActivityType.BANK_DEPOSIT_STRIPE
        : AccountActivityType.BANK_WITHDRAWAL;
  }
}
