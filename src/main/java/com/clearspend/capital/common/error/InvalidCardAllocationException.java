package com.clearspend.capital.common.error;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;

public class InvalidCardAllocationException extends RuntimeException {
  public InvalidCardAllocationException(
      final TypedId<CardId> cardId, final TypedId<AllocationId> allocationId) {
    super("Card (%s) does not allow Allocation (%s)".formatted(cardId, allocationId));
  }
}
