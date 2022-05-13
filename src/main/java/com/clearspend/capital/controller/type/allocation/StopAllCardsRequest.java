package com.clearspend.capital.controller.type.allocation;

import lombok.NonNull;

public record StopAllCardsRequest(
    boolean applyToChildAllocations,
    boolean cancelVirtualCards,
    @NonNull StopPhysicalCardsType stopPhysicalCardsType) {
  public enum StopPhysicalCardsType {
    CANCEL,
    UNLINK,
    NONE
  }
}
