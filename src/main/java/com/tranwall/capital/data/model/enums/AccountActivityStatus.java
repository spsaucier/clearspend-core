package com.tranwall.capital.data.model.enums;

public enum AccountActivityStatus {
  // for holds (both ACH and card transactions)
  PENDING,

  // declined card transactions
  DECLINED,

  // approved card transactions
  APPROVED,

  // for non-card transactions (e.g. reallocations)
  PROCESSED,
}
