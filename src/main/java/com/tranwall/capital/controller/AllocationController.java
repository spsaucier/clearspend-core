package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
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
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @Validated @RequestBody CreateAllocationRequest request) {
    AllocationRecord allocationRecord =
        allocationService.createAllocation(
            request.getProgramId(),
            businessId,
            request.getParentAllocationId(),
            request.getName(),
            request.getCurrency());

    return new CreateAllocationResponse(allocationRecord.allocation().getId());
  }

  @GetMapping("/{allocationId}")
  private Allocation getAllocation(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @PathVariable(value = "allocationId")
          @ApiParam(
              required = true,
              name = "allocationId",
              value = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {
    AllocationRecord allocationRecord =
        allocationService.getAllocation(businessService.retrieveBusiness(businessId), allocationId);

    return new Allocation(
        allocationRecord.allocation().getId(),
        allocationRecord.allocation().getProgramId(),
        allocationRecord.allocation().getName(),
        Account.of(allocationRecord.account()));
  }

  @GetMapping("/{allocationId}/children")
  private List<Allocation> getAllocationChildren(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @PathVariable(value = "allocationId")
          @ApiParam(
              required = true,
              name = "allocationId",
              value = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {
    return allocationService
        .getAllocationChildren(businessService.retrieveBusiness(businessId), allocationId)
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
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @PathVariable(value = "allocationId")
          @ApiParam(
              required = true,
              name = "allocationId",
              value = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @RequestBody @Validated AllocationFundCardRequest request) {

    AccountReallocateFundsRecord reallocateFundsRecord =
        allocationService.reallocateAllocationFunds(
            businessService.retrieveBusiness(businessId),
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