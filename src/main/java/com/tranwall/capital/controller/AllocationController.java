package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.account.Account;
import com.tranwall.capital.controller.type.allocation.Allocation;
import com.tranwall.capital.controller.type.allocation.AllocationFundCardRequest;
import com.tranwall.capital.controller.type.allocation.AllocationFundCardResponse;
import com.tranwall.capital.controller.type.allocation.CreateAllocationRequest;
import com.tranwall.capital.controller.type.allocation.CreateAllocationResponse;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

  @PostMapping("")
  private CreateAllocationResponse createAllocation(
      @Validated @RequestBody CreateAllocationRequest request) {
    AllocationRecord allocationRecord =
        allocationService.createAllocation(
            request.getProgramId(),
            CurrentUser.get().businessId(),
            request.getParentAllocationId(),
            request.getName(),
            request.getAmount().toAmount());

    return new CreateAllocationResponse(allocationRecord.allocation().getId());
  }

  @GetMapping("/{allocationId}")
  private Allocation getAllocation(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {
    AllocationRecord allocationRecord =
        allocationService.getAllocation(
            businessService.retrieveBusiness(CurrentUser.get().businessId()), allocationId);

    return new Allocation(
        allocationRecord.allocation().getId(),
        allocationRecord.allocation().getProgramId(),
        allocationRecord.allocation().getName(),
        Account.of(allocationRecord.account()));
  }

  @GetMapping("/{allocationId}/children")
  private List<Allocation> getAllocationChildren(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {
    return allocationService
        .getAllocationChildren(
            businessService.retrieveBusiness(CurrentUser.get().businessId()), allocationId)
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

  @PostMapping("/{allocationId}/transactions")
  private AllocationFundCardResponse reallocateAllocationFunds(
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
            businessService.retrieveBusiness(CurrentUser.get().businessId()),
            allocationId,
            request.getAllocationAccountId(),
            request.getCardId(),
            request.getFundsTransactType(),
            request.getAmount().toAmount());

    return new AllocationFundCardResponse(
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment().getId(),
        Amount.of(reallocateFundsRecord.fromAccount().getLedgerBalance()),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment().getId(),
        Amount.of(reallocateFundsRecord.toAccount().getLedgerBalance()));
  }
}
