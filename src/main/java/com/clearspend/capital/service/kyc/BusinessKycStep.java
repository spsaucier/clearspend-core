package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.client.stripe.types.Capabilities;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.review.StripeAccountDisabledReason;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.BusinessService.OnboardingBusinessOp;
import com.clearspend.capital.service.TwilioService;
import com.stripe.model.Account.Requirements;
import com.stripe.model.Account.Requirements.Errors;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Transactional
public abstract class BusinessKycStep {

  @Autowired TwilioService twilioService;
  @Autowired BusinessService businessService;
  @Autowired BusinessOwnerRepository businessOwnerRepository;
  @Autowired StripeClient stripeClient;

  public static final String EXTERNAL_ACCOUNT_CODE_REQUIREMENT = "external_account";
  public static final String ACTIVE = "active";
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
  public static final String IDENTITY = "individual";
  public static final String COMPANY_TAX_ID = "company.tax_id";

  abstract boolean support(Requirements requirements, Business business, Account account);

  abstract List<String> execute(
      Requirements requirements, Business business, Account account, boolean sendEmail);

  @OnboardingBusinessOp(
      reviewer = "Craig Miller",
      explanation = "This method uses the Business for onboarding tasks")
  public void updateBusiness(
      TypedId<BusinessId> businessId,
      BusinessStatus status,
      BusinessOnboardingStep onboardingStep,
      KnowYourBusinessStatus knowYourBusinessStatus) {
    businessService.updateBusinessForOnboarding(
        businessId, status, onboardingStep, knowYourBusinessStatus);
  }

  public Boolean businessOrCompanyRequirementsMatch(String s) {
    return s.startsWith(BUSINESS_PROFILE_DETAILS_REQUIRED)
        || (s.startsWith(COMPANY)
            && !s.endsWith(COMPANY_OWNERS_PROVIDED)
            && !s.endsWith(COMPANY_TAX_ID));
  }

  public Boolean personRequirementsMatch(String s) {
    return s.startsWith(REPRESENTATIVE_DETAILS_REQUIRED)
        || s.startsWith(OWNERS_DETAILS_REQUIRED)
        || s.startsWith(IDENTITY)
        || (s.startsWith(PERSON) && !s.endsWith(DOCUMENT) && !s.endsWith(SSN_LAST_4));
  }

  public List<String> extractErrorMessages(Requirements requirements) {
    return requirements.getErrors() != null
        ? requirements.getErrors().stream().map(Errors::getReason).distinct().toList()
        : null;
  }

  private Boolean noOtherCheckRequiredForKYCStep(Requirements requirements) {
    boolean disabled =
        requirements.getDisabledReason() != null
            && (requirements.getDisabledReason().startsWith(REQUIREMENTS)
                || requirements
                    .getDisabledReason()
                    .startsWith(StripeAccountDisabledReason.LISTED.getCodeName()));
    boolean pastDue =
        CollectionUtils.isEmpty(requirements.getPastDue())
            || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(requirements.getPastDue().get(0))
                && requirements.getPastDue().size() == 1
                && disabled);
    boolean eventuallyDue =
        CollectionUtils.isEmpty(requirements.getEventuallyDue())
            || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(requirements.getEventuallyDue().get(0))
                && requirements.getEventuallyDue().size() == 1
                && disabled);
    boolean currentlyDue =
        CollectionUtils.isEmpty(requirements.getCurrentlyDue())
            || (EXTERNAL_ACCOUNT_CODE_REQUIREMENT.equals(requirements.getCurrentlyDue().get(0))
                && requirements.getCurrentlyDue().size() == 1
                && disabled);
    return pastDue
        && eventuallyDue
        && currentlyDue
        && CollectionUtils.isEmpty(requirements.getErrors());
  }

  private Boolean activeAccountCapabilities(Account account) {
    Capabilities accountCapabilities = account.getCapabilities();
    return accountCapabilities != null
        && ACTIVE.equals(accountCapabilities.getCardIssuing())
        && ACTIVE.equals(accountCapabilities.getCardPayments())
        && ACTIVE.equals(accountCapabilities.getTransfers())
        && ACTIVE.equals(accountCapabilities.getTreasury())
        && ACTIVE.equals(accountCapabilities.getUsBankAccountAchPayments());
  }

  public Boolean applicationRequireAdditionalCheck(Account account, Requirements requirements) {
    return !noOtherCheckRequiredForKYCStep(requirements) || !activeAccountCapabilities(account);
  }
}
