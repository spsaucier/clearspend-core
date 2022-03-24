package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.business.UpdateBusiness;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.StripeData;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.BusinessStatusReason;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService.AllocationDetailsRecord;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import com.google.errorprone.annotations.RestrictedApi;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

  public @interface StripeBusinessOp {

    String reviewer();

    String explanation();
  }

  public @interface OnboardingBusinessOp {

    String reviewer();

    String explanation();
  }

  public static final String ACTIVE = "active";

  private final BusinessRepository businessRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessLimitService businessLimitService;
  private final RetrievalService retrievalService;
  private final ExpenseCategoryService expenseCategoryService;

  private final StripeClient stripeClient;

  public record BusinessRecord(Business business, Account businessAccount) {}

  public record BusinessAndStripeAccount(
      Business business, com.stripe.model.Account stripeAccount) {}

  @Transactional
  BusinessAndStripeAccount createBusiness(
      TypedId<BusinessId> businessId,
      BusinessType businessType,
      String businessEmail,
      ConvertBusinessProspect convertBusinessProspect,
      TosAcceptance tosAcceptance) {
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
            new StripeData(FinancialAccountState.NOT_READY, tosAcceptance),
            AccountingSetupStep.ADD_CREDIT_CARD);
    if (businessId != null) {
      business.setId(businessId);
    }

    business.setBusinessName(convertBusinessProspect.getBusinessName());
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

    expenseCategoryService.createDefaultCategoriesForBusiness(business.getId());

    return new BusinessAndStripeAccount(business, account);
  }

  @Transactional
  Business updateBusinessWithCodatCompanyRef(
      TypedId<BusinessId> businessId, String codatCompanyRef) {
    Business business = retrieveBusiness(businessId, true);

    business.setCodatCompanyRef(codatCompanyRef);

    return businessRepository.save(business);
  }

  @Transactional
  Business updateBusinessWithCodatConnectionId(
      TypedId<BusinessId> businessId, String codatConnectionId) {
    Business business = retrieveBusiness(businessId, true);

    business.setCodatConnectionId(codatConnectionId);

    return businessRepository.save(business);
  }

  Business deleteCodatConnectionForBusiness(TypedId<BusinessId> businessId) {
    Business business = retrieveBusiness(businessId, true);

    business.setCodatConnectionId(null);

    return businessRepository.save(business);
  }

  Business updateCodatCreditCardForBusiness(
      TypedId<BusinessId> businessId, String codatCreditCardId) {
    Business business = retrieveBusiness(businessId, true);

    business.setCodatCreditCardId(codatCreditCardId);

    return businessRepository.save(business);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public Business updateBusinessAccountingSetupStep(
      TypedId<BusinessId> businessId, AccountingSetupStep accountingSetupStep) {
    Business business = retrieveBusiness(businessId, true);

    business.setAccountingSetupStep(accountingSetupStep);

    return businessRepository.save(business);
  }

  @Transactional
  @RestrictedApi(
      explanation = "This method is used exclusively to support onboarding operations",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {OnboardingBusinessOp.class})
  public Business updateBusinessForOnboarding(
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
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public com.clearspend.capital.controller.type.business.Business updateBusiness(
      TypedId<BusinessId> businessId, UpdateBusiness updateBusiness) {
    Business business = retrieveBusiness(businessId, true);

    BeanUtils.setNotNull(updateBusiness.getBusinessType(), business::setType);
    BeanUtils.setNotNull(updateBusiness.getLegalName(), business::setLegalName);
    BeanUtils.setNotNull(updateBusiness.getBusinessName(), business::setBusinessName);
    BeanUtils.setNotNull(
        updateBusiness.getEmployerIdentificationNumber(),
        business::setEmployerIdentificationNumber);
    BeanUtils.setNotNull(updateBusiness.getDescription(), business::setDescription);
    BeanUtils.setNotNull(updateBusiness.getUrl(), business::setUrl);

    if (updateBusiness.getMcc() != null) {
      business.setMcc(updateBusiness.getMcc());
    }

    if (updateBusiness.getAddress() != null) {
      business.setClearAddress(ClearAddress.of(updateBusiness.getAddress().toAddress()));
    }

    businessRepository.save(business);

    stripeClient.updateAccount(business);

    return new com.clearspend.capital.controller.type.business.Business(business);
  }

  @Transactional
  @RestrictedApi(
      explanation =
          "This method is used by Stripe operations for which user permissions are not available.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {StripeBusinessOp.class})
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

  private Business retrieveBusiness(TypedId<BusinessId> businessId, boolean mustExist) {
    return retrievalService.retrieveBusiness(businessId, mustExist);
  }

  Business retrieveBusinessForService(
      final TypedId<BusinessId> businessId, final boolean mustExist) {
    return retrieveBusiness(businessId, mustExist);
  }

  @RestrictedApi(
      explanation = "This method is used exclusively to support onboarding operations",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {OnboardingBusinessOp.class})
  public Business retrieveBusinessForOnboarding(
      final TypedId<BusinessId> businessId, final boolean mustExist) {
    return retrieveBusiness(businessId, mustExist);
  }

  @RestrictedApi(
      explanation =
          "This method is used by Stripe operations for which user permissions are not available.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {StripeBusinessOp.class})
  public Business retrieveBusinessForStripe(
      final TypedId<BusinessId> businessIdTypedId, final boolean mustExist) {
    return retrieveBusiness(businessIdTypedId, mustExist);
  }

  @PostAuthorize(
      "hasRootPermission(returnObject.businessId, 'VIEW_OWN') or hasRootPermission(returnObject.businessId, 'READ') or hasGlobalPermission('CUSTOMER_SERVICE|GLOBAL_READ')")
  public Business getBusiness(final TypedId<BusinessId> businessId, final boolean mustExist) {
    return retrieveBusiness(businessId, mustExist);
  }

  Business retrieveBusinessByEmployerIdentificationNumber(String employerIdentificationNumber) {
    return businessRepository
        .findByEmployerIdentificationNumber(employerIdentificationNumber)
        .orElse(null);
  }

  @RestrictedApi(
      explanation =
          "This method is used by Stripe operations for which user permissions are not available.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {StripeBusinessOp.class})
  public Business retrieveBusinessByStripeFinancialAccount(String stripeFinancialAccountRef) {
    return businessRepository
        .findByStripeFinancialAccountRef(stripeFinancialAccountRef)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, stripeFinancialAccountRef));
  }

  @RestrictedApi(
      explanation =
          "This method is used by Stripe operations for which user permissions are not available.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {StripeBusinessOp.class})
  public Business retrieveBusinessByStripeAccountReference(String stripeAccountReference) {
    return businessRepository
        .findByStripeAccountRef(stripeAccountReference)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, stripeAccountReference));
  }

  BusinessRecord getBusiness(TypedId<BusinessId> businessId) {
    Business business = retrieveBusiness(businessId, true);
    Account account = allocationService.getRootAllocation(businessId).account();
    return new BusinessRecord(business, account);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_FUNDS')")
  public AccountReallocateFundsRecord reallocateBusinessFunds(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
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

    User user = retrievalService.retrieveUser(businessId, userId);

    accountActivityService.recordReallocationAccountActivity(
        allocationFromRecord.allocation(),
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment(),
        user);
    accountActivityService.recordReallocationAccountActivity(
        allocationToRecord.allocation(),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment(),
        user);

    return reallocateFundsRecord;
  }

  @Transactional
  AdjustmentAndHoldRecord applyFee(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Amount amount,
      String notes) {
    amount.ensureNonNegative();

    Allocation allocation = allocationService.retrieveAllocation(businessId, allocationId);
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        accountService.applyFee(allocation.getAccountId(), amount);

    accountActivityService.recordApplyFeeActivity(
        allocation, adjustmentAndHoldRecord.adjustment(), notes);

    return adjustmentAndHoldRecord;
  }
}
