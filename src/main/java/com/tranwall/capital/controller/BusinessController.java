package com.tranwall.capital.controller;

import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.account.Account;
import com.tranwall.capital.controller.type.allocation.Allocation;
import com.tranwall.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.tranwall.capital.controller.type.business.Business;
import com.tranwall.capital.controller.type.business.reallocation.BusinessFundAllocationRequest;
import com.tranwall.capital.controller.type.business.reallocation.BusinessFundAllocationResponse;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.BusinessService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

  private final AllocationService allocationService;
  private final BusinessService businessService;

  @PostMapping("/transactions")
  private BusinessFundAllocationResponse reallocateBusinessFunds(
      @RequestBody @Validated BusinessFundAllocationRequest request) {

    AccountReallocateFundsRecord reallocateFundsRecord =
        businessService.reallocateBusinessFunds(
            CurrentUser.get().businessId(),
            request.getAllocationId(),
            request.getAccountId(),
            request.getFundsTransactType(),
            request.getAmount().toAmount());

    return new BusinessFundAllocationResponse(
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment().getId(),
        Amount.of(reallocateFundsRecord.fromAccount().getLedgerBalance()),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment().getId(),
        Amount.of(reallocateFundsRecord.toAccount().getLedgerBalance()));
  }

  @GetMapping("/allocations")
  private List<Allocation> getRootAllocations() {
    return allocationService
        .getAllocationChildren(
            businessService.retrieveBusiness(CurrentUser.get().businessId()), null)
        .stream()
        .map(
            e ->
                new Allocation(
                    e.allocation().getId(),
                    e.allocation().getProgramId(),
                    e.allocation().getName(),
                    Account.of(e.account())))
        .collect(Collectors.toList());
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
                    e.allocation().getId(),
                    e.allocation().getProgramId(),
                    e.allocation().getName(),
                    Account.of(e.account())))
        .collect(Collectors.toList());
  }

  @GetMapping
  private Business getBusiness() {
    return new Business(businessService.retrieveBusiness(CurrentUser.get().businessId()));
  }
}
