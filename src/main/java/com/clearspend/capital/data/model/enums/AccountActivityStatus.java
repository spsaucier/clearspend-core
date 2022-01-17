package com.clearspend.capital.data.model.enums;

public enum AccountActivityStatus {
  // for holds (both ACH and card transactions)
  PENDING,

  // declined card transactions (decline record)
  DECLINED,

  // approved card transactions (an adjustment is created)
  APPROVED,

  // for void or authorizations timeout webhook from Stripe (remove a hold)
  CANCELED,

  // for refund or credit card transactions (an adjustment)
  CREDIT,

  // for non-card transactions (e.g. reallocations) (an adjustment)
  PROCESSED,
}
