package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.BusinessStatusReason;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AllocationService.AllocationDetailsRecord;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import com.stripe.model.Account.Capabilities;
import com.stripe.model.Account.Requirements;
import com.stripe.model.Account.Requirements.Errors;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

  public static final String EXTERNAL_ACCOUNT_CODE_REQUIREMENT = "external_account";
  public static final String ACTIVE = "active";
  public static final String TREASURY = "treasury";
  public static final String US_BANK_ACCOUNT_ACH_PAYMENTS = "us_bank_account_ach_payments";
  public static final String REQUIREMENTS = "requirements";
  public static final String REJECTED = "rejected";
  public static final String BUSINESS_PROFILE_DETAILS_REQUIRED = "business_profile";
  public static final String OWNERS_DETAILS_REQUIRED = "owners";
  public static final String REPRESENTATIVE_DETAILS_REQUIRED = "representative";
  public static final String PERSON = "person";
  public static final String DOCUMENT = "document";

  private final BusinessRepository businessRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessLimitService businessLimitService;
  private final MccGroupService mccGroupService;

  private final StripeClient stripeClient;

  public record BusinessRecord(Business business, Account businessAccount) {}

  public record BusinessAndStripeMessagesRecord(
      Business business, List<String> stripeAccountCreationMessages) {}

  @Transactional
  public BusinessAndStripeMessagesRecord createBusiness(
      TypedId<BusinessId> businessId,
      BusinessType businessType,
      ConvertBusinessProspect convertBusinessProspect,
      String tosAcceptanceIp) {
    Business business =
        new Business(
            convertBusinessProspect.getLegalName(),
            businessType,
            ClearAddress.of(convertBusinessProspect.getAddress()),
            convertBusinessProspect.getEmployerIdentificationNumber(),
            Currency.USD,
            BusinessOnboardingStep.BUSINESS_OWNERS,
            KnowYourBusinessStatus.PENDING,
            BusinessStatus.ONBOARDING,
            BusinessStatusReason.NONE,
            convertBusinessProspect.getMerchantType().getMcc(),
            tosAcceptanceIp);
    if (businessId != null) {
      business.setId(businessId);
    }

    business.setDescription(convertBusinessProspect.getDescription());
    business.setBusinessPhone(
        new RequiredEncryptedString(convertBusinessProspect.getBusinessPhone()));
    // for SMB without online presence we will set a default as ClearSpend URL
    business.setUrl(
        StringUtils.isEmpty(convertBusinessProspect.getUrl())
            ? "https://www.clearspend.com/"
            : convertBusinessProspect.getUrl());

    business = businessRepository.save(business);

    // stripe account creation
    com.stripe.model.Account account = stripeClient.createAccount(business);
    business.setStripeAccountReference(account.getId());

    // TODO: The step below probably should be moved to a later phase, after KYB/KYC,
    // to be finalized after KYB/KYC part will be ready
    business.setStripeFinancialAccountRef(
        stripeClient
            .createFinancialAccount(business.getId(), business.getStripeAccountReference())
            .getId());

    businessLimitService.initializeBusinessLimit(business.getId());
    mccGroupService.initializeMccGroups(business.getId());

    // validate and update business based on stripe account requirements
    List<String> stripeAccountErrorMessages =
        updateBusinessAccordingToStripeAccountRequirements(business, account);

    return new BusinessAndStripeMessagesRecord(business, stripeAccountErrorMessages);
  }

  @Transactional
  public List<String> updateBusinessAccordingToStripeAccountRequirements(
      Business business, com.stripe.model.Account account) {
    if (business.getStatus() == BusinessStatus.ONBOARDING
        && List.of(
                BusinessOnboardingStep.BUSINESS_OWNERS,
                BusinessOnboardingStep.REVIEW,
                BusinessOnboardingStep.SOFT_FAIL)
            .contains(business.getOnboardingStep())) {
      Requirements requirements = account.getRequirements();

      // This is for testing case, in real case we should not have null requirements
      if (requirements == null) {
        return new ArrayList<>();
      }

      Capabilities accountCapabilities = account.getCapabilities();
      boolean activeAccountCapabilities =
          accountCapabilities != null
              && ACTIVE.equals(accountCapabilities.getCardIssuing())
              && ACTIVE.equals(accountCapabilities.getCardPayments())
              && ACTIVE.equals(accountCapabilities.getTransfers());
      // TODO: gb: check how to get these capabilities
      //            &&
      // ACTIVE.equals(accountCapabilities.getRawJsonObject().get(TREASURY).getAsString())
      //            && ACTIVE.equals(
      //                accountCapabilities
      //                    .getRawJsonObject()
      //                    .get(US_BANK_ACCOUNT_ACH_PAYMENTS)
      //                    .getAsString());
      // TODO - discuss eventualy due and required capabilities
      boolean disabled =
          requirements.getDisabledReason() != null
              && requirements.getDisabledReason().startsWith(REQUIREMENTS);
      boolean noOtherCheckRequiredForKYCStep =
          (CollectionUtils.isEmpty(requirements.getPastDue())
                  || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(requirements.getPastDue().get(0))
                      && requirements.getPastDue().size() == 1
                      && disabled))
              && (CollectionUtils.isEmpty(requirements.getEventuallyDue())
                  || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(
                          requirements.getEventuallyDue().get(0))
                      && requirements.getEventuallyDue().size() == 1
                      && disabled))
              && (CollectionUtils.isEmpty(requirements.getCurrentlyDue())
                  || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(
                          requirements.getCurrentlyDue().get(0))
                      && requirements.getCurrentlyDue().size() == 1
                      && disabled))
              && CollectionUtils.isEmpty(requirements.getPendingVerification())
              && CollectionUtils.isEmpty(requirements.getErrors());

      boolean applicationReadyForNextStep =
          noOtherCheckRequiredForKYCStep && activeAccountCapabilities;

      boolean applicationRejected =
          StringUtils.isNotEmpty(requirements.getDisabledReason())
              && requirements.getDisabledReason().startsWith(REJECTED);

      boolean applicationRequireAdditionalCheck =
          !activeAccountCapabilities && !noOtherCheckRequiredForKYCStep;

      boolean applicationIsInReviewState =
          !activeAccountCapabilities && noOtherCheckRequiredForKYCStep;

      if (applicationRejected) {
        if (business.getStatus() != BusinessStatus.CLOSED) {
          updateBusiness(
              business.getId(), BusinessStatus.CLOSED, null, KnowYourBusinessStatus.FAIL);
          // TODO:gb: send email for fail application
        }
      } else if (applicationReadyForNextStep) {
        if (business.getOnboardingStep() != BusinessOnboardingStep.LINK_ACCOUNT
            || business.getOnboardingStep() != BusinessOnboardingStep.TRANSFER_MONEY
            || business.getOnboardingStep() != BusinessOnboardingStep.COMPLETE) {
          updateBusiness(
              business.getId(),
              null,
              BusinessOnboardingStep.LINK_ACCOUNT,
              KnowYourBusinessStatus.PASS);
          // TODO:gb: send email for success application KYC
        }
      } else if (applicationIsInReviewState) {
        if (business.getOnboardingStep() != BusinessOnboardingStep.REVIEW) {
          updateBusiness(
              business.getId(), null, BusinessOnboardingStep.REVIEW, KnowYourBusinessStatus.REVIEW);
          // TODO:gb: send email for REVIEW state
        }
      } else if (applicationRequireAdditionalCheck) {
        // when additional checks are required,
        // we will move business onboarding status depending on the stripe required information
        if ((!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
                && requirements.getCurrentlyDue().stream()
                    .anyMatch(
                        s ->
                            s.startsWith(REPRESENTATIVE_DETAILS_REQUIRED)
                                || s.startsWith(OWNERS_DETAILS_REQUIRED)
                                || (s.startsWith(PERSON) && !s.endsWith(DOCUMENT))))
            || (!CollectionUtils.isEmpty(requirements.getPastDue())
                && requirements.getPastDue().stream()
                    .anyMatch(
                        s ->
                            s.startsWith(REPRESENTATIVE_DETAILS_REQUIRED)
                                || s.startsWith(OWNERS_DETAILS_REQUIRED)
                                || (s.startsWith(PERSON) && !s.endsWith(DOCUMENT))))
            || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
                && requirements.getEventuallyDue().stream()
                    .anyMatch(
                        s ->
                            s.startsWith(REPRESENTATIVE_DETAILS_REQUIRED)
                                || s.startsWith(OWNERS_DETAILS_REQUIRED)
                                || (s.startsWith(PERSON) && !s.endsWith(DOCUMENT))))) {
          if (business.getOnboardingStep() != BusinessOnboardingStep.BUSINESS_OWNERS) {
            updateBusiness(
                business.getId(),
                null,
                BusinessOnboardingStep.BUSINESS_OWNERS,
                KnowYourBusinessStatus.PENDING);
            // TODO:gb: send email for additional information required about business owners
          }
        } else if ((!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
                && requirements.getCurrentlyDue().stream()
                    .anyMatch(s -> s.startsWith(BUSINESS_PROFILE_DETAILS_REQUIRED)))
            || (!CollectionUtils.isEmpty(requirements.getPastDue())
                && requirements.getPastDue().stream()
                    .anyMatch(s -> s.startsWith(BUSINESS_PROFILE_DETAILS_REQUIRED)))
            || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
                && requirements.getEventuallyDue().stream()
                    .anyMatch(s -> s.startsWith(BUSINESS_PROFILE_DETAILS_REQUIRED)))) {
          if (business.getOnboardingStep() != BusinessOnboardingStep.BUSINESS) {
            updateBusiness(
                business.getId(),
                null,
                BusinessOnboardingStep.BUSINESS,
                KnowYourBusinessStatus.PENDING);
            // TODO:gb: send email for additional information required about business
          }
        } else if ((!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
                && requirements.getCurrentlyDue().stream().anyMatch(s -> s.endsWith(DOCUMENT)))
            || (!CollectionUtils.isEmpty(requirements.getPastDue())
                && requirements.getPastDue().stream().anyMatch(s -> s.endsWith(DOCUMENT)))
            || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
                && requirements.getEventuallyDue().stream().anyMatch(s -> s.endsWith(DOCUMENT)))
            || (!CollectionUtils.isEmpty(requirements.getPendingVerification())
                && requirements.getPendingVerification().stream()
                    .anyMatch(s -> s.endsWith(DOCUMENT)))) {
          if (business.getOnboardingStep() != BusinessOnboardingStep.SOFT_FAIL) {
            updateBusiness(
                business.getId(),
                null,
                BusinessOnboardingStep.SOFT_FAIL,
                KnowYourBusinessStatus.REVIEW);
            // TODO:gb: send email for required documents to review
          }
        } else {
          if (business.getOnboardingStep() != BusinessOnboardingStep.REVIEW) {
            updateBusiness(
                business.getId(),
                null,
                BusinessOnboardingStep.REVIEW,
                KnowYourBusinessStatus.REVIEW);
            // TODO:gb: send email for review application KYC
          }
        }
      } else {
        // this case should be developed
        log.error("This case should be checked {}", account.toJson());
        // TODO:gb: check if is possible to arrive on unknown state
      }

      return extractErrorMessages(requirements);
    }
    return new ArrayList<>();
  }

  private List<String> extractErrorMessages(Requirements requirements) {
    return requirements.getErrors() != null
        ? requirements.getErrors().stream().map(Errors::getReason).toList()
        : null;
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

  public Business retrieveBusinessByStripeFinancialAccount(String stripeFinancialAccountRef) {
    return businessRepository
        .findByStripeFinancialAccountRef(stripeFinancialAccountRef)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, stripeFinancialAccountRef));
  }

  public Business retrieveBusinessByStripeAccountReference(String stripeAccountReference) {
    return businessRepository
        .findByStripeAccountReference(stripeAccountReference)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, stripeAccountReference));
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
