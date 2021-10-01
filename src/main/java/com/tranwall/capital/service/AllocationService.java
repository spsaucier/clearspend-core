package com.tranwall.capital.service;

import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.Currency;
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

  private final AccountService accountService;

  public record AllocationRecord(Allocation allocation, Account acccount) {}

  public AllocationRecord createAllocation(
      UUID programId, UUID businessId, UUID parentAllocationId, String name, Currency currency) {
    Allocation allocation = new Allocation(programId, businessId, name);

    if (parentAllocationId != null) {
      allocation.setParentAllocationId(parentAllocationId);
      Allocation parent =
          allocationRepository
              .findById(parentAllocationId)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Parent allocation not found: " + parentAllocationId));

      if (parent.getBusinessId().equals(businessId)) {
        throw new IllegalArgumentException(
            "Parent allocation not owned by business: " + parentAllocationId);
      }

      allocation.setAncestorAllocationIds(
          allocationRepository.retrieveAncestorAllocationIds(parentAllocationId));
    }

    allocation = allocationRepository.save(allocation);
    Account account =
        accountService.createAccount(
            businessId, AccountType.ALLOCATION, allocation.getId(), currency);

    return new AllocationRecord(allocation, account);
  }
}
