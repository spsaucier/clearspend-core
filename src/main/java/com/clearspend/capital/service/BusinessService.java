package com.clearspend.capital.service;

import com.clearspend.capital.client.alloy.AlloyClient;
import com.clearspend.capital.client.alloy.AlloyClient.KybEvaluationResponse;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Alloy;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AlloyTokenType;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.BusinessStatusReason;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.AlloyRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AllocationService.AllocationDetailsRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
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

  private final BusinessRepository businessRepository;
  private final AlloyRepository alloyRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessLimitService businessLimitService;
  private final MccGroupService mccGroupService;

  private final AlloyClient alloyClient;
  private final StripeClient stripeClient;

  public record BusinessRecord(Business business, Account businessAccount) {}

  public record BusinessAndAllocationsRecord(
      Business business, AllocationRecord allocationRecord) {}

  @SneakyThrows
  @Transactional
  public Business createBusiness(
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

    KybEvaluationResponse kybEvaluationResponse = alloyClient.onboardBusiness(business);
    business.setKnowYourBusinessStatus(kybEvaluationResponse.status());

    business = businessRepository.save(business);

    if (kybEvaluationResponse.status() == KnowYourBusinessStatus.REVIEW) {
      alloyRepository.save(
          new Alloy(
              business.getId(),
              null,
              AlloyTokenType.BUSINESS,
              kybEvaluationResponse.entityToken()));
    }

    if (business.getKnowYourBusinessStatus() == KnowYourBusinessStatus.FAIL) {
      business.setStatus(BusinessStatus.CLOSED);
    } else {
      business.setExternalRef(stripeClient.createAccount(business).getId());

      // TODO: The step below probably should be moved to a later phase, after KYB/KYC,
      // to be finalized after KYB/KYC part will be ready
      business.setStripeFinancialAccountRef(
          stripeClient.createFinancialAccount(business.getId(), business.getExternalRef()).getId());

      businessLimitService.initializeBusinessSpendLimit(business.getId());
      mccGroupService.initializeMccGroups(business.getId());
    }

    return business;
  }

  @Transactional
  public Business updateBusiness(
      TypedId<BusinessId> businessId,
      BusinessStatus status,
      BusinessOnboardingStep onboardingStep,
      KnowYourBusinessStatus knowYourBusinessStatus) {
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

    if (knowYourBusinessStatus != null) {
      business.setKnowYourBusinessStatus(knowYourBusinessStatus);
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
      @NonNull TypedId<AllocationId> allocationIdFrom,
      @NonNull TypedId<AllocationId> allocationIdTo,
      Amount amount) {
    amount.ensureNonNegative();

    BusinessRecord businessRecord = getBusiness(businessId);
    AllocationDetailsRecord allocationFromRecord =
        allocationService.getAllocation(businessRecord.business, allocationIdFrom);
    AllocationDetailsRecord allocationToRecord =
        allocationService.getAllocation(businessRecord.business, allocationIdTo);

    AccountReallocateFundsRecord reallocateFundsRecord =
        accountService.reallocateFunds(
            allocationFromRecord.account().getId(), allocationToRecord.account().getId(), amount);

    accountActivityService.recordReallocationAccountActivity(
        allocationFromRecord.allocation(),
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment());
    accountActivityService.recordReallocationAccountActivity(
        allocationToRecord.allocation(),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment());

    return reallocateFundsRecord;
  }
}
