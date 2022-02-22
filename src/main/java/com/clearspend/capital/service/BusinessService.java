package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.StripeData;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.BusinessStatusReason;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
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
import org.apache.logging.log4j.util.Strings;
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
  public static final String SSN_LAST_4 = ".ssn_last_4";
  public static final String COMPANY_OWNERS_PROVIDED = "company.owners_provided";
  public static final String COMPANY = "company";

  private final BusinessRepository businessRepository;
  private final BusinessOwnerRepository businessOwnerRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessLimitService businessLimitService;
  private final TwilioService twilioService;
  private final RetrievalService retrievalService;

  private final StripeClient stripeClient;

  public record BusinessRecord(Business business, Account businessAccount) {}

  public record BusinessAndStripeAccount(
      Business business, com.stripe.model.Account stripeAccount) {}

  @Transactional
  public BusinessAndStripeAccount createBusiness(
      TypedId<BusinessId> businessId,
      BusinessType businessType,
      String businessEmail,
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
            new StripeData(FinancialAccountState.NOT_READY, tosAcceptanceIp),
            AccountingSetupStep.ADD_CREDIT_CARD);
    if (businessId != null) {
      business.setId(businessId);
    }

    business.setDescription(convertBusinessProspect.getDescription());
    business.setBusinessPhone(
        new RequiredEncryptedString(convertBusinessProspect.getBusinessPhone()));
    business.setBusinessEmail(new RequiredEncryptedString(businessEmail));
    // for SMB without online presence we will set a default as ClearSpend URL
    business.setUrl(
        StringUtils.isEmpty(convertBusinessProspect.getUrl())
            ? "https://www.clearspend.com/"
            : convertBusinessProspect.getUrl());

    business = businessRepository.save(business);

    // stripe account creation
    com.stripe.model.Account account = stripeClient.createAccount(business);
    business.getStripeData().setAccountRef(account.getId());
    // TODO hot-fix start the financial account creation process
    business
        .getStripeData()
        .setFinancialAccountRef(
            stripeClient.createFinancialAccount(business.getId(), account.getId()).getId());

    businessLimitService.initializeBusinessLimit(business.getId());

    return new BusinessAndStripeAccount(business, account);
  }

  @Transactional
  public List<String> updateBusinessAccordingToStripeAccountRequirements(
      Business business, com.stripe.model.Account account) {
    if (business.getStatus() != BusinessStatus.ONBOARDING
        || !List.of(
                BusinessOnboardingStep.BUSINESS_OWNERS,
                BusinessOnboardingStep.REVIEW,
                BusinessOnboardingStep.SOFT_FAIL)
            .contains(business.getOnboardingStep())) {

      if (Strings.isBlank(business.getStripeData().getFinancialAccountRef())) {
        // TODO: The step below probably should be moved to a later phase, after KYB/KYC,
        // to be finalized after KYB/KYC part will be ready
        business
            .getStripeData()
            .setFinancialAccountRef(
                stripeClient
                    .createFinancialAccount(
                        business.getId(), business.getStripeData().getAccountRef())
                    .getId());
      }

      return new ArrayList<>();
    }

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
                || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(requirements.getEventuallyDue().get(0))
                    && requirements.getEventuallyDue().size() == 1
                    && disabled))
            && (CollectionUtils.isEmpty(requirements.getCurrentlyDue())
                || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(requirements.getCurrentlyDue().get(0))
                    && requirements.getCurrentlyDue().size() == 1
                    && disabled))
            && CollectionUtils.isEmpty(requirements.getPendingVerification())
            && CollectionUtils.isEmpty(requirements.getErrors());

    boolean applicationRejected =
        StringUtils.isNotEmpty(requirements.getDisabledReason())
            && requirements.getDisabledReason().startsWith(REJECTED);
    if (applicationRejected) {
      if (business.getStatus() != BusinessStatus.CLOSED) {
        updateBusiness(business.getId(), BusinessStatus.CLOSED, null, KnowYourBusinessStatus.FAIL);
        BusinessOwner businessOwner =
            businessOwnerRepository
                .findByBusinessIdAndEmailHash(
                    business.getId(),
                    HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
                .orElse(
                    businessOwnerRepository.findByBusinessId(business.getId()).stream()
                        .findAny()
                        .orElseThrow());

        List<String> reasons = extractErrorMessages(requirements);
        twilioService.sendKybKycFailEmail(
            business.getBusinessEmail().getEncrypted(),
            businessOwner.getFirstName().getEncrypted(),
            reasons);
        return reasons;
      }

      return extractErrorMessages(requirements);
    }

    boolean applicationReadyForNextStep =
        noOtherCheckRequiredForKYCStep && activeAccountCapabilities;
    if (applicationReadyForNextStep) {
      if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.LINK_ACCOUNT)) {
        updateBusiness(
            business.getId(),
            null,
            BusinessOnboardingStep.LINK_ACCOUNT,
            KnowYourBusinessStatus.PASS);
        BusinessOwner businessOwner =
            businessOwnerRepository
                .findByBusinessIdAndEmailHash(
                    business.getId(),
                    HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
                .orElse(
                    businessOwnerRepository.findByBusinessId(business.getId()).stream()
                        .findAny()
                        .orElseThrow());

        twilioService.sendKybKycPassEmail(
            business.getBusinessEmail().getEncrypted(),
            businessOwner.getFirstName().getEncrypted());
      }

      return extractErrorMessages(requirements);
    }

    boolean applicationIsInReviewState =
        !activeAccountCapabilities && noOtherCheckRequiredForKYCStep;
    if (applicationIsInReviewState) {
      if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.REVIEW)) {
        updateBusiness(
            business.getId(), null, BusinessOnboardingStep.REVIEW, KnowYourBusinessStatus.REVIEW);
        // TODO:gb: send email for REVIEW state
        BusinessOwner businessOwner =
            businessOwnerRepository
                .findByBusinessIdAndEmailHash(
                    business.getId(),
                    HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
                .orElse(
                    businessOwnerRepository.findByBusinessId(business.getId()).stream()
                        .findAny()
                        .orElseThrow());

        List<String> reasons = extractErrorMessages(requirements);
        twilioService.sendKybKycReviewStateEmail(
            business.getBusinessEmail().getEncrypted(),
            businessOwner.getFirstName().getEncrypted());
        return reasons;
      }

      return extractErrorMessages(requirements);
    }

    boolean applicationRequireAdditionalCheck =
        !activeAccountCapabilities && !noOtherCheckRequiredForKYCStep;
    if (applicationRequireAdditionalCheck) {
      // when additional checks are required,
      // we will move business onboarding status depending on the stripe required information
      if ((!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
              && requirements.getCurrentlyDue().stream()
                  .anyMatch(this::businessOrCompanyRequirementsMatch))
          || (!CollectionUtils.isEmpty(requirements.getPastDue())
              && requirements.getPastDue().stream()
                  .anyMatch(this::businessOrCompanyRequirementsMatch))
          || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
              && requirements.getEventuallyDue().stream()
                  .anyMatch(this::businessOrCompanyRequirementsMatch))) {
        if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.BUSINESS)) {
          updateBusiness(
              business.getId(),
              null,
              BusinessOnboardingStep.BUSINESS,
              KnowYourBusinessStatus.PENDING);
          // TODO:gb: send email for additional information required about business
          BusinessOwner businessOwner =
              businessOwnerRepository
                  .findByBusinessIdAndEmailHash(
                      business.getId(),
                      HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
                  .orElse(
                      businessOwnerRepository.findByBusinessId(business.getId()).stream()
                          .findAny()
                          .orElseThrow());

          List<String> reasons = extractErrorMessages(requirements);
          twilioService.sendKybKycRequireAdditionalInfoEmail(
              business.getBusinessEmail().getEncrypted(),
              businessOwner.getFirstName().getEncrypted(),
              reasons);
          return reasons;
        }
      } else if ((!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
              && requirements.getCurrentlyDue().stream().anyMatch(this::personRequirementsMatch))
          || (!CollectionUtils.isEmpty(requirements.getPastDue())
              && requirements.getPastDue().stream().anyMatch(this::personRequirementsMatch))
          || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
              && requirements.getEventuallyDue().stream()
                  .anyMatch(this::personRequirementsMatch))) {
        if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.BUSINESS_OWNERS)) {
          updateBusiness(
              business.getId(),
              null,
              BusinessOnboardingStep.BUSINESS_OWNERS,
              KnowYourBusinessStatus.PENDING);
          // TODO:gb: send email for additional information required about business owners
          BusinessOwner businessOwner =
              businessOwnerRepository
                  .findByBusinessIdAndEmailHash(
                      business.getId(),
                      HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
                  .orElse(
                      businessOwnerRepository.findByBusinessId(business.getId()).stream()
                          .findAny()
                          .orElseThrow());

          List<String> reasons = extractErrorMessages(requirements);
          twilioService.sendKybKycRequireAdditionalInfoEmail(
              business.getBusinessEmail().getEncrypted(),
              businessOwner.getFirstName().getEncrypted(),
              reasons);
          return reasons;
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
        if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.SOFT_FAIL)) {
          updateBusiness(
              business.getId(),
              null,
              BusinessOnboardingStep.SOFT_FAIL,
              KnowYourBusinessStatus.REVIEW);
          // TODO:gb: send email for required documents to review
          BusinessOwner businessOwner =
              businessOwnerRepository
                  .findByBusinessIdAndEmailHash(
                      business.getId(),
                      HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
                  .orElse(
                      businessOwnerRepository.findByBusinessId(business.getId()).stream()
                          .findAny()
                          .orElseThrow());

          List<String> reasons = extractErrorMessages(requirements);
          twilioService.sendKybKycRequireDocumentsEmail(
              business.getBusinessEmail().getEncrypted(),
              businessOwner.getFirstName().getEncrypted(),
              reasons);
          return reasons;
        }
      } else {
        if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.REVIEW)) {
          updateBusiness(
              business.getId(), null, BusinessOnboardingStep.REVIEW, KnowYourBusinessStatus.REVIEW);
          // TODO:gb: send email for review application KYC
          BusinessOwner businessOwner =
              businessOwnerRepository
                  .findByBusinessIdAndEmailHash(
                      business.getId(),
                      HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
                  .orElse(
                      businessOwnerRepository.findByBusinessId(business.getId()).stream()
                          .findAny()
                          .orElseThrow());

          List<String> reasons = extractErrorMessages(requirements);
          twilioService.sendKybKycReviewStateEmail(
              business.getBusinessEmail().getEncrypted(),
              businessOwner.getFirstName().getEncrypted());
          return reasons;
        }
      }

      return extractErrorMessages(requirements);
    }

    // this case should be developed
    log.error("This case should be checked {}", account.toJson());
    // TODO:gb: check if is possible to arrive on unknown state

    return extractErrorMessages(requirements);
  }

  private Boolean businessOrCompanyRequirementsMatch(String s) {
    return s.startsWith(BUSINESS_PROFILE_DETAILS_REQUIRED)
        || (s.startsWith(COMPANY) && !s.endsWith(COMPANY_OWNERS_PROVIDED));
  }

  private Boolean personRequirementsMatch(String s) {
    return s.startsWith(REPRESENTATIVE_DETAILS_REQUIRED)
        || s.startsWith(OWNERS_DETAILS_REQUIRED)
        || (s.startsWith(PERSON) && !s.endsWith(DOCUMENT) && !s.endsWith(SSN_LAST_4));
  }

  private List<String> extractErrorMessages(Requirements requirements) {
    return requirements.getErrors() != null
        ? requirements.getErrors().stream().map(Errors::getReason).toList()
        : null;
  }

  @Transactional
  public Business updateBusinessWithCodatCompanyRef(
      TypedId<BusinessId> businessId, String codatCompanyRef) {
    Business business = retrieveBusiness(businessId, true);

    business.setCodatCompanyRef(codatCompanyRef);

    return businessRepository.save(business);
  }

  @Transactional
  public Business updateBusinessAccountingSetupStep(
      TypedId<BusinessId> businessId, AccountingSetupStep accountingSetupStep) {
    Business business = retrieveBusiness(businessId, true);

    business.setAccountingSetupStep(accountingSetupStep);

    return businessRepository.save(business);
  }

  @Transactional
  public Business updateBusiness(
      TypedId<BusinessId> businessId,
      BusinessStatus status,
      BusinessOnboardingStep onboardingStep,
      KnowYourBusinessStatus knowYourBusinessStatus) {
    Business business = retrieveBusiness(businessId, true);

    BeanUtils.setNotNull(onboardingStep, business::setOnboardingStep);
    BeanUtils.setNotNull(status, business::setStatus);
    BeanUtils.setNotNull(knowYourBusinessStatus, business::setKnowYourBusinessStatus);

    return business;
  }

  @Transactional
  public Business updateBusinessStripeData(
      TypedId<BusinessId> businessId,
      String stripeAccountRef,
      String stripeFinancialAccountRef,
      FinancialAccountState stripeFinancialAccountState,
      String stripeAccountNumber,
      String stripeRoutringNumber) {
    Business business = retrieveBusiness(businessId, true);
    StripeData stripeData = business.getStripeData();

    BeanUtils.setNotNull(stripeAccountRef, stripeData::setAccountRef);
    BeanUtils.setNotNull(stripeFinancialAccountRef, stripeData::setFinancialAccountRef);
    BeanUtils.setNotNull(stripeFinancialAccountState, stripeData::setFinancialAccountState);
    BeanUtils.setNotNull(
        stripeAccountNumber, v -> stripeData.setBankAccountNumber(new RequiredEncryptedString(v)));
    BeanUtils.setNotNull(
        stripeRoutringNumber, v -> stripeData.setBankRoutingNumber(new RequiredEncryptedString(v)));

    return business;
  }

  public Business retrieveBusiness(TypedId<BusinessId> businessId, boolean mustExist) {
    return retrievalService.retrieveBusiness(businessId, mustExist);
  }

  public Business retrieveBusinessByEmployerIdentificationNumber(
      String employerIdentificationNumber) {
    return businessRepository
        .findByEmployerIdentificationNumber(employerIdentificationNumber)
        .orElse(null);
  }

  public Business retrieveBusinessByStripeFinancialAccount(String stripeFinancialAccountRef) {
    return businessRepository
        .findByStripeFinancialAccountRef(stripeFinancialAccountRef)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, stripeFinancialAccountRef));
  }

  public Business retrieveBusinessByStripeAccountReference(String stripeAccountReference) {
    return businessRepository
        .findByStripeAccountRef(stripeAccountReference)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, stripeAccountReference));
  }

  public BusinessRecord getBusiness(TypedId<BusinessId> businessId) {
    Business business = retrieveBusiness(businessId, true);
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

  @Transactional
  public void triggerAccountValidationAfterPersonsProvided(
      List<BusinessOwner> businessOwners, Business business, String stripeAccountReference) {
    com.stripe.model.Account updatedAccount =
        stripeClient.triggerAccountValidationAfterPersonsProvided(
            stripeAccountReference,
            businessOwners.stream()
                .filter(businessOwnerData -> businessOwnerData.getRelationshipOwner() != null)
                .anyMatch(BusinessOwner::getRelationshipOwner),
            businessOwners.stream()
                .filter(businessOwnerData -> businessOwnerData.getRelationshipExecutive() != null)
                .anyMatch(BusinessOwner::getRelationshipExecutive));

    updateBusinessAccordingToStripeAccountRequirements(business, updatedAccount);
  }
}
