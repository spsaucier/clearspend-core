package com.tranwall.capital.controller;

import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.account.Account;
import com.tranwall.capital.controller.type.allocation.Allocation;
import com.tranwall.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.tranwall.capital.controller.type.business.Business;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/businesses")
@RequiredArgsConstructor
public class BusinessController {

  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessService businessService;

  @GetMapping("/accounts")
  private Account getRootAllocationAccount() {
    return Account.of(
        allocationService.getRootAllocation(CurrentUser.get().businessId()).account());
  }

  @GetMapping("/allocations")
  private Allocation getRootAllocation() {
    AllocationRecord rootAllocation =
        allocationService.getRootAllocation(CurrentUser.get().businessId());
    return new Allocation(
        rootAllocation.allocation().getId(),
        rootAllocation.allocation().getName(),
        Account.of(rootAllocation.account()));
  }

  @PostMapping("/allocations")
  private List<Allocation> searchBusinessAllocations(
      @RequestBody @Validated SearchBusinessAllocationRequest request) {
    return allocationService
        .searchBusinessAllocations(
            businessService.retrieveBusiness(CurrentUser.get().businessId()), request.getName())
        .stream()
        .map(
            e ->
                new Allocation(
                    e.allocation().getId(), e.allocation().getName(), Account.of(e.account())))
        .collect(Collectors.toList());
  }

  @GetMapping
  private ResponseEntity<Business> getBusiness() {

    return ResponseEntity.ok(
        new Business(businessService.retrieveBusiness(CurrentUser.get().businessId())));
  }
}
