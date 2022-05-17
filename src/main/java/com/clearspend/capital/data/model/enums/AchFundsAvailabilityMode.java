package com.clearspend.capital.data.model.enums;

public enum AchFundsAvailabilityMode {

  // funds become available as soon as the hold expires
  STANDARD,

  // funds become available as soon as we have a notification from Stripe
  FAST,

  // money becomes available immediately if main financial account has enough extra money (total -
  // sum of all business funds)
  IMMEDIATE;
}
