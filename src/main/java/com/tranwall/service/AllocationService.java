package com.tranwall.service;

import com.tranwall.data.model.Allocation;
import com.tranwall.data.repository.AllocationRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationService {

  @NonNull private final AllocationRepository allocationRepository;

  Allocation createAllocation(
      UUID programId, UUID businessId, UUID parentAllocationId, String name) {
    Allocation allocation = new Allocation(programId, businessId, parentAllocationId, null, name);
    if (parentAllocationId != null) {
      Optional<Allocation> parent = allocationRepository.findById(parentAllocationId);
      if (parent.isEmpty()) {
        throw new IllegalArgumentException("Parent allocation not found: " + parentAllocationId);
      }
      if (parent.get().getBusinessId().equals(businessId)) {
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
