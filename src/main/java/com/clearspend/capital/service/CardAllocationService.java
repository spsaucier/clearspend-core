package com.clearspend.capital.service;

import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.CardAllocation;
import com.clearspend.capital.data.repository.CardAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardAllocationService {
  private final RetrievalService retrievalService;
  private final CardAllocationRepository cardAllocationRepository;

  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public CardAllocation addAllocationToCard(
      final TypedId<CardId> cardId, final TypedId<AllocationId> allocationId) {
    if (retrievalService.retrieveAllocation(allocationId).isArchived()) {
      throw new InvalidRequestException("Allocation is archived");
    }

    return cardAllocationRepository.save(new CardAllocation(cardId, allocationId));
  }
}
