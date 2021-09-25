package com.tranwall.data.model.enums;

// ONBOARDING (initial) -> ACTIVE
// ONBOARDING -> CLOSED (terminal)
// ACTIVE -> SUSPENDED
// ACTIVE -> CLOSED (terminal)
// SUSPENDED -> CLOSED (terminal)
public enum BusinessStatus {
  ONBOARDING,
  ACTIVE,
  SUSPENDED,
  CLOSED,
}
