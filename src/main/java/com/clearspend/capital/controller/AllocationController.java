package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.controller.type.allocation.Allocation;
import com.clearspend.capital.controller.type.allocation.AllocationDetailsResponse;
import com.clearspend.capital.controller.type.allocation.AllocationFundCardRequest;
import com.clearspend.capital.controller.type.allocation.AllocationFundCardResponse;
import com.clearspend.capital.controller.type.allocation.CreateAllocationRequest;
import com.clearspend.capital.controller.type.allocation.CreateAllocationResponse;
import com.clearspend.capital.controller.type.allocation.UpdateAllocationRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationDetailsRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/allocations")
@RequiredArgsConstructor
public class AllocationController {

  private final AllocationService allocationService;
  private final BusinessService businessService;
  private final UserService userService;

  @PostMapping("")
  CreateAllocationResponse createAllocation(
      @Validated @RequestBody CreateAllocationRequest request) {
    AllocationRecord allocationRecord =
        allocationService.createAllocation(
            CurrentUser.get().businessId(),
            request.getParentAllocationId(),
            request.getName(),
            userService.retrieveUser(request.getOwnerId()),
            request.getAmount().toAmount(),
            CurrencyLimit.toMap(request.getLimits()),
            request.getDisabledMccGroups(),
            request.getDisabledPaymentTypes());

    return new CreateAllocationResponse(allocationRecord.allocation().getId());
  }

  @GetMapping("/{allocationId}")
  AllocationDetailsResponse getAllocation(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {
    AllocationDetailsRecord allocationRecord =
        allocationService.getAllocation(
            businessService.retrieveBusiness(CurrentUser.get().businessId(), true), allocationId);

    return AllocationDetailsResponse.of(allocationRecord);
  }

  @PatchMapping("/{allocationId}")
  AllocationDetailsResponse updateAllocation(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @RequestBody @Validated UpdateAllocationRequest request) {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();

    allocationService.updateAllocation(
        businessId,
        allocationId,
        request.getName(),
        request.getParentAllocationId(),
        request.getOwnerId(),
        CurrencyLimit.toMap(request.getLimits()),
        request.getDisabledMccGroups(),
        request.getDisabledPaymentTypes());

    AllocationDetailsRecord allocationRecord =
        allocationService.getAllocation(
            businessService.retrieveBusiness(businessId, true), allocationId);

    return AllocationDetailsResponse.of(allocationRecord);
  }

  @GetMapping("/{allocationId}/children")
  List<Allocation> getAllocationChildren(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {
    return allocationService
        .getAllocationChildren(
            businessService.retrieveBusiness(CurrentUser.get().businessId(), true), allocationId)
        .stream()
        .map(
            e ->
                new Allocation(
                    e.allocation().getId(),
                    e.allocation().getName(),
                    e.allocation().getOwnerId(),
                    Account.of(e.account())))
        .collect(Collectors.toList());
  }

  @PostMapping("/{allocationId}/transactions")
  AllocationFundCardResponse reallocateAllocationFunds(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @RequestBody @Validated AllocationFundCardRequest request) {

    AccountReallocateFundsRecord reallocateFundsRecord =
        allocationService.reallocateAllocationFunds(
            businessService.retrieveBusiness(CurrentUser.get().businessId(), true),
            allocationId,
            request.getAllocationAccountId(),
            request.getCardId(),
            request.getReallocationType(),
            request.getAmount().toAmount());

    return new AllocationFundCardResponse(
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment().getId(),
        Amount.of(reallocateFundsRecord.fromAccount().getLedgerBalance()),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment().getId(),
        Amount.of(reallocateFundsRecord.toAccount().getLedgerBalance()));
  }
}
