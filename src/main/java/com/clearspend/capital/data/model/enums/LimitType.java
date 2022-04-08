package com.clearspend.capital.data.model.enums;

public enum LimitType {
  /** Limit for pulling money to Clearspend from an external bank account */
  ACH_DEPOSIT,

  /** Limit for pushing money from Clearspend to an external bank account */
  ACH_WITHDRAW,

  /** Limit for any type of purchase transaction */
  PURCHASE
}
