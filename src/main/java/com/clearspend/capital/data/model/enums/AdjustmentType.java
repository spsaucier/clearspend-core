package com.clearspend.capital.data.model.enums;

public enum AdjustmentType {
  DEPOSIT, // an ACH deposit
  MANUAL, // a manual credit/debit of an allocation account (typically customer service credits)
  NETWORK, // a card payment via network message
  REALLOCATE, // a transfer between allocation <-> allocation, allocation <-> card accounts
  WITHDRAW, // an ACH withdrawal
  FEE, // a fee for some operations
  RETURN // an ACH/unsolicited card return
}
