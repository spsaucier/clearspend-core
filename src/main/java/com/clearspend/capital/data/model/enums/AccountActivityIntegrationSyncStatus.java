package com.clearspend.capital.data.model.enums;

import com.clearspend.capital.common.error.InvalidStateTransitionException;
import com.google.common.collect.ImmutableSetMultimap;
import lombok.NonNull;

public enum AccountActivityIntegrationSyncStatus {
  SYNCED_LOCKED,
  READY,
  NOT_READY;

  private static final ImmutableSetMultimap<
          AccountActivityIntegrationSyncStatus, AccountActivityIntegrationSyncStatus>
      validTransitions =
          ImmutableSetMultimap
              .<AccountActivityIntegrationSyncStatus, AccountActivityIntegrationSyncStatus>builder()
              .putAll(NOT_READY, READY)
              .putAll(READY, SYNCED_LOCKED)
              .build();

  public AccountActivityIntegrationSyncStatus validTransition(
      @NonNull AccountActivityIntegrationSyncStatus next) {
    if (!validTransitions.containsEntry(this, next)) {
      throw new InvalidStateTransitionException(this, next);
    }
    return next;
  }
}
