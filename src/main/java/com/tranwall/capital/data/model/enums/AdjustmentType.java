package com.tranwall.capital.data.model.enums;

public enum AdjustmentType {
  DEPOSIT, // an ACH deposit
  REALLOCATE, // a transfer between business <-> allocation, allocation <-> card accounts
  NETWORK, // a card payment via network message
  WITHDRAW, // an ACH withdrawal
}
