package com.tranwall.capital.data.model.enums;

import com.google.common.collect.ImmutableSetMultimap;
import com.tranwall.capital.common.error.InvalidStateTransitionException;
import lombok.NonNull;

// OPEN (initial) -> BLOCKED
// BLOCKED -> OPEN
// BLOCKED -> RETIRED (terminal)
public enum CardStatus {
  OPEN,
  BLOCKED,
  RETIRED;

  private static final ImmutableSetMultimap<CardStatus, CardStatus> validTransitions =
      ImmutableSetMultimap.<CardStatus, CardStatus>builder()
          .putAll(OPEN, BLOCKED, RETIRED)
          .putAll(BLOCKED, OPEN, RETIRED)
          .build();

  public CardStatus validTransition(@NonNull CardStatus next) {
    if (!validTransitions.containsEntry(this, next)) {
      throw new InvalidStateTransitionException(this, next);
    }

    return next;
  }
}
