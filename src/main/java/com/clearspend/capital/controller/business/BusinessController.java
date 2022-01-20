package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.controller.type.allocation.Allocation;
import com.clearspend.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.business.reallocation.BusinessFundAllocationResponse;
import com.clearspend.capital.controller.type.business.reallocation.BusinessReallocationRequest;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

  private final AllocationService allocationService;
  private final BusinessService businessService;

  @PostMapping("/transactions")
  private BusinessFundAllocationResponse reallocateBusinessFunds(
      @RequestBody @Validated BusinessReallocationRequest request) {

    AccountReallocateFundsRecord reallocateFundsRecord =
        businessService.reallocateBusinessFunds(
            CurrentUser.get().businessId(),
            request.getAllocationIdFrom(),
            request.getAllocationIdTo(),
            request.getAmount().toAmount());

    return new BusinessFundAllocationResponse(
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment().getId(),
        Amount.of(reallocateFundsRecord.fromAccount().getLedgerBalance()),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment().getId(),
        Amount.of(reallocateFundsRecord.toAccount().getLedgerBalance()));
  }

  @GetMapping("/accounts")
  private Account getRootAllocationAccount() {
    return Account.of(
        allocationService.getRootAllocation(CurrentUser.get().businessId()).account());
  }

  @GetMapping("/allocations")
  private List<Allocation> getBusinessAllocations() {
    Map<TypedId<AllocationId>, Allocation> result =
        allocationService
            .searchBusinessAllocations(
                businessService.retrieveBusiness(CurrentUser.get().businessId()))
            .stream()
            .map(Allocation::of)
            .collect(Collectors.toMap(Allocation::getAllocationId, Function.identity()));

    // calculate children
    result
        .values()
        .forEach(
            allocation -> {
              TypedId<AllocationId> parentAllocationId = allocation.getParentAllocationId();
              if (parentAllocationId != null) {
                result
                    .get(parentAllocationId)
                    .getChildrenAllocationIds()
                    .add(allocation.getAllocationId());
              }
            });

    return new ArrayList<>(result.values());
  }

  @PostMapping("/allocations")
  private List<Allocation> searchBusinessAllocations(
      @RequestBody @Validated SearchBusinessAllocationRequest request) {
    return allocationService
        .searchBusinessAllocations(
            businessService.retrieveBusiness(CurrentUser.get().businessId()), request.getName())
        .stream()
        .map(Allocation::of)
        .collect(Collectors.toList());
  }

  @GetMapping
  private ResponseEntity<Business> getBusiness() {

    return ResponseEntity.ok(
        new Business(businessService.retrieveBusiness(CurrentUser.get().businessId())));
  }
}
