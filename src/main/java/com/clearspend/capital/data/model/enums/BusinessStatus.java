package com.clearspend.capital.data.model.enums;

// ONBOARDING (initial) -> ACTIVE
// ONBOARDING -> CLOSED (terminal)
// ACTIVE -> SUSPENDED
// ACTIVE -> SUSPENDED_EXPENDITURE
// ACTIVE -> CLOSED (terminal)
// SUSPENDED_EXPENDITURE -> ACTIVE/SUSPENDED/CLOSED
// SUSPENDED -> CLOSED (terminal)
public enum BusinessStatus {
  ONBOARDING,
  ACTIVE,
  SUSPENDED,
  SUSPENDED_EXPENDITURE,
  CLOSED,
}
