package com.tranwall.capital.data.model.enums;

// OPEN (initial) -> BLOCKED
// BLOCKED -> OPEN
// BLOCKED -> RETIRED (terminal)
public enum CardStatus {
  OPEN,
  BLOCKED,
  RETIRED,
}
