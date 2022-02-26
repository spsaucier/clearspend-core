package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.controller.type.allocation.Allocation;
import com.clearspend.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.clearspend.capital.controller.type.allocation.UpdateAllocationBalanceRequest;
import com.clearspend.capital.controller.type.allocation.UpdateAllocationBalanceResponse;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.business.BusinessLimit;
import com.clearspend.capital.controller.type.business.UpdateBusiness;
import com.clearspend.capital.controller.type.business.accounting.UpdateBusinessAccountingStepRequest;
import com.clearspend.capital.controller.type.business.reallocation.BusinessFundAllocationResponse;
import com.clearspend.capital.controller.type.business.reallocation.BusinessReallocationRequest;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.BusinessLimitService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/businesses")
@RequiredArgsConstructor
public class BusinessController {

  public static final EnumSet<BusinessOnboardingStep> COMPLETABLE_ONBOARDING_STEPS =
      EnumSet.of(BusinessOnboardingStep.LINK_ACCOUNT, BusinessOnboardingStep.TRANSFER_MONEY);
  private final AllocationService allocationService;
  private final BusinessService businessService;
  private final BusinessLimitService businessLimitService;

  @PostMapping("/transactions")
  BusinessFundAllocationResponse reallocateBusinessFunds(
      @RequestBody @Validated BusinessReallocationRequest request) {

    AccountReallocateFundsRecord reallocateFundsRecord =
        businessService.reallocateBusinessFunds(
            CurrentUser.getBusinessId(),
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
  Account getRootAllocationAccount() {
    return Account.of(allocationService.getRootAllocation(CurrentUser.getBusinessId()).account());
  }

  @GetMapping("/allocations")
  List<Allocation> getBusinessAllocations() {
    Map<TypedId<AllocationId>, Allocation> result =
        allocationService
            .searchBusinessAllocations(
                businessService.retrieveBusiness(CurrentUser.getBusinessId(), true))
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
  List<Allocation> searchBusinessAllocations(
      @RequestBody @Validated SearchBusinessAllocationRequest request) {
    return allocationService
        .searchBusinessAllocations(
            businessService.retrieveBusiness(CurrentUser.getBusinessId(), true), request.getName())
        .stream()
        .map(Allocation::of)
        .toList();
  }

  @PostMapping("{businessId}/allocations/{allocationId}/transactions")
  UpdateAllocationBalanceResponse updateAllocationBalance(
      @PathVariable(value = "businessId")
          @Parameter(
              required = true,
              name = "businessId",
              description = "ID of the business record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessId> businessId,
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @RequestBody @Validated UpdateAllocationBalanceRequest request) {
    AdjustmentRecord adjustmentRecord =
        allocationService.updateAllocationBalance(
            businessId, allocationId, request.getAmount().toAmount(), request.getNotes());
    return new UpdateAllocationBalanceResponse(
        adjustmentRecord.adjustment().getId(),
        Amount.of(adjustmentRecord.account().getLedgerBalance()));
  }

  @GetMapping
  ResponseEntity<Business> getBusiness() {
    return ResponseEntity.ok(
        new Business(businessService.retrieveBusiness(CurrentUser.getBusinessId(), false)));
  }

  @GetMapping("/business-limit")
  BusinessLimit getBusinessLimit() {
    return BusinessLimit.of(
        businessLimitService.retrieveBusinessLimit(CurrentUser.getBusinessId()));
  }

  @PostMapping("/complete-onboarding")
  ResponseEntity<?> completeOnboarding() {
    TypedId<BusinessId> businessId = CurrentUser.getBusinessId();
    Business business = new Business(businessService.retrieveBusiness(businessId, false));

    if (COMPLETABLE_ONBOARDING_STEPS.contains(business.getOnboardingStep())) {
      businessService.updateBusiness(
          businessId, BusinessStatus.ACTIVE, BusinessOnboardingStep.COMPLETE, null);
    } else {
      throw new RuntimeException(
          "Cannot complete business onboarding due to the non completable state "
              + business.getOnboardingStep());
    }

    return ResponseEntity.ok().build();
  }

  @PostMapping("/update")
  ResponseEntity<?> updateBusinessDetails(@RequestBody @Validated UpdateBusiness request) {
    TypedId<BusinessId> businessId = CurrentUser.getBusinessId();
    return ResponseEntity.ok(businessService.updateBusiness(businessId, request));
  }

  @PostMapping("/accounting-step")
  ResponseEntity<Business> updateAccountingSetupStepForBusiness(
      @RequestBody @Validated
          UpdateBusinessAccountingStepRequest updateBusinessAccountingStepRequest) {
    return ResponseEntity.ok(
        new Business(
            businessService.updateBusinessAccountingSetupStep(
                CurrentUser.getBusinessId(),
                updateBusinessAccountingStepRequest.getAccountingSetupStep())));
  }
}
