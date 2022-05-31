package com.clearspend.capital.service;

import com.clearspend.capital.common.error.InvalidCardAllocationException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.CardAllocation;
import com.clearspend.capital.data.repository.CardAllocationRepository;
import java.util.Optional;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardAllocationService {
  private final RetrievalService retrievalService;
  private final CardAllocationRepository cardAllocationRepository;

  public record AddCardAllocationRecord(
      @NonNull CardAllocation cardAllocation, boolean wasCreated) {
    public <T> Optional<T> ifWasCreated(final Function<CardAllocation, T> wasCreatedAction) {
      if (wasCreated) {
        return Optional.ofNullable(wasCreatedAction.apply(cardAllocation));
      }
      return Optional.empty();
    }
  }

  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public AddCardAllocationRecord addAllocationToCard(
      final TypedId<CardId> cardId, final TypedId<AllocationId> allocationId) {
    if (retrievalService.retrieveAllocation(allocationId).isArchived()) {
      throw new InvalidRequestException("Allocation is archived");
    }

    return cardAllocationRepository
        .findByCardIdAndAllocationId(cardId, allocationId)
        .map(cardAllocation -> new AddCardAllocationRecord(cardAllocation, false))
        .orElseGet(
            () ->
                new AddCardAllocationRecord(
                    cardAllocationRepository.save(new CardAllocation(cardId, allocationId)), true));
  }

  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public Optional<CardAllocation> removeAllocationFromCard(
      final TypedId<CardId> cardId, final TypedId<AllocationId> allocationId) {
    final Optional<CardAllocation> cardAllocation =
        cardAllocationRepository.findByCardIdAndAllocationId(cardId, allocationId);
    cardAllocation.ifPresent(
        ca -> cardAllocationRepository.removeByCardIdAndAllocationId(cardId, allocationId));
    return cardAllocation;
  }

  @SuppressWarnings("ReturnValueIgnored")
  void allowLinkingCardToAllocation(
      final TypedId<CardId> cardId, final TypedId<AllocationId> allocationId) {
    cardAllocationRepository
        .findByCardIdAndAllocationId(cardId, allocationId)
        .orElseThrow(() -> new InvalidCardAllocationException(cardId, allocationId));
  }
}
