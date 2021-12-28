package com.clearspend.capital.data.model.enums;

import com.clearspend.capital.common.error.InvalidStateTransitionException;
import com.google.common.collect.ImmutableSetMultimap;
import lombok.NonNull;

// ACTIVE (initial) -> INACTIVE
// INACTIVE -> ACTIVE
// INACTIVE -> CANCELLED (terminal)
public enum CardStatus {
  ACTIVE,
  INACTIVE,
  CANCELLED;

  private static final ImmutableSetMultimap<CardStatus, CardStatus> validTransitions =
      ImmutableSetMultimap.<CardStatus, CardStatus>builder()
          .putAll(ACTIVE, INACTIVE, CANCELLED)
          .putAll(INACTIVE, ACTIVE, CANCELLED)
          .build();

  public CardStatus validTransition(@NonNull CardStatus next) {
    if (!validTransitions.containsEntry(this, next)) {
      throw new InvalidStateTransitionException(this, next);
    }

    return next;
  }
}
