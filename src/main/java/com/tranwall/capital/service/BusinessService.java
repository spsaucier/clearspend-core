package com.tranwall.capital.service;

import com.tranwall.capital.client.alloy.AlloyClient;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.ClearAddress;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessReallocationType;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.BusinessStatusReason;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.ProgramRepository;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

  public static final String DEFAULT_PROGRAM_ID = "6faf3838-b2d7-422c-8d6f-c2294ebc73b4";
  private final BusinessRepository businessRepository;
  private final ProgramRepository programRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessLimitService businessLimitService;

  private final AlloyClient alloyClient;

  public record BusinessRecord(Business business, Account businessAccount) {}

  public record BusinessAndAllocationsRecord(
      Business business, AllocationRecord allocationRecord) {}

  @SneakyThrows
  @Transactional
  public BusinessAndAllocationsRecord createBusiness(
      TypedId<BusinessId> businessId,
      String legalName,
      BusinessType type,
      Address address,
      String employerIdentificationNumber,
      String email,
      String phone,
      Currency currency) {
    Business business =
        new Business(
            legalName,
            type,
            ClearAddress.of(address),
            employerIdentificationNumber,
            currency,
            BusinessOnboardingStep.BUSINESS_OWNERS,
            KnowYourBusinessStatus.PENDING,
            BusinessStatus.ONBOARDING,
            BusinessStatusReason.NONE);
    if (businessId != null) {
      business.setId(businessId);
    }
    business.setBusinessEmail(new RequiredEncryptedString(email));
    business.setBusinessPhone(new RequiredEncryptedString(phone));

    business.setKnowYourBusinessStatus(alloyClient.onboardBusiness(business).status());

    business = businessRepository.save(business);

    businessLimitService.initializeBusinessSpendLimit(business.getId());

    AllocationRecord allocationRecord =
        allocationService.createRootAllocation(
            business.getId(), business.getLegalName() + " - root");

    return new BusinessAndAllocationsRecord(business, allocationRecord);
  }

  @Transactional
  public Business updateBusiness(
      TypedId<BusinessId> businessId,
      BusinessStatus status,
      BusinessOnboardingStep onboardingStep) {
    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    if (onboardingStep != null) {
      business.setOnboardingStep(onboardingStep);
    }

    if (status != null) {
      business.setStatus(status);
    }

    return businessRepository.save(business);
  }

  public Business retrieveBusiness(TypedId<BusinessId> businessId) {
    return businessRepository
        .findById(businessId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));
  }

  public BusinessRecord getBusiness(TypedId<BusinessId> businessId) {
    Business business = retrieveBusiness(businessId);
    Account account = allocationService.getRootAllocation(businessId).account();
    return new BusinessRecord(business, account);
  }

  @Transactional
  public AccountReallocateFundsRecord reallocateBusinessFunds(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull TypedId<AccountId> accountId,
      @NonNull BusinessReallocationType businessReallocationType,
      Amount amount) {
    BusinessRecord businessRecord = getBusiness(businessId);
    AllocationRecord allocationRecord =
        allocationService.getAllocation(businessRecord.business, allocationId);
    if (!allocationRecord.account().getId().equals(accountId)) {
      throw new IdMismatchException(
          IdType.ACCOUNT_ID, accountId, allocationRecord.account().getId());
    }
    if (allocationRecord.allocation().getParentAllocationId() == null) {
      throw new IllegalArgumentException(
          String.format("Allocation must be a child allocation: %s", allocationId));
    }

    AccountReallocateFundsRecord reallocateFundsRecord =
        switch (businessReallocationType) {
          case ALLOCATION_TO_BUSINESS -> accountService.reallocateFunds(
              allocationRecord.account().getId(), businessRecord.businessAccount.getId(), amount);
          case BUSINESS_TO_ALLOCATION -> accountService.reallocateFunds(
              businessRecord.businessAccount.getId(), allocationRecord.account().getId(), amount);
        };

    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation(),
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment());
    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation(),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment());

    return reallocateFundsRecord;
  }
}
