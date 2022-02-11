package com.clearspend.capital.data.model.enums;

public enum LimitType {
  /** Limit for pushing money from Clearspend to an external bank account */
  ACH_DEPOSIT,

  /** Limit for pulling money to Clearspend from an external bank account */
  ACH_WITHDRAW,

  /** Limit for pushing money to Clearspend from an external bank account */
  ACH_PUSH_IN,

  /** Limit for pulling money from Clearspend to an external bank account */
  ACH_PULL_OUT,

  /** Limit for any type of purchase transaction */
  PURCHASE,

  /** Limit for the ATM withdraw */
  ATM_WITHDRAW
}
