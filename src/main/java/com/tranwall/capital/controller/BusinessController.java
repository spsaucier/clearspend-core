package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.account.Account;
import com.tranwall.capital.controller.type.allocation.Allocation;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @RequestBody @Validated BusinessFundAllocationRequest request) {

    AccountReallocateFundsRecord reallocateFundsRecord =
        businessService.reallocateBusinessFunds(
            businessId,
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
  private List<Allocation> getRootAllocations(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId) {
    return allocationService
        .getAllocationChildren(businessService.retrieveBusiness(businessId), null)
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
}
