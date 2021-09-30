package com.tranwall.capital.service;

import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.repository.AllocationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationService {

  private final AllocationRepository allocationRepository;

  public Allocation createAllocation(
      UUID programId, UUID businessId, UUID parentAllocationId, String name) {
    Allocation allocation = new Allocation(programId, businessId, parentAllocationId, null, name);

    if (parentAllocationId != null) {
      Allocation parent = allocationRepository.findById(parentAllocationId)
          .orElseThrow(() -> new IllegalArgumentException("Parent allocation not found: " + parentAllocationId));

      if (parent.getBusinessId().equals(businessId)) {
        throw new IllegalArgumentException(
            "Parent allocation not owned by business: " + parentAllocationId);
      }

      allocation.setAncestorAllocationIds(
          allocationRepository.retrieveAncestorAllocationIds(parentAllocationId));
    }

    // FIXME(kuchlein): need to create allocation account

    return allocationRepository.save(allocation);
  }
}
