package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.service.TwilioService.TwilioKycKybOp;
import com.stripe.model.Account.Requirements;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(6)
public class BusinessKycStep6BusinessSoftFail extends BusinessKycStep {

  @Override
  public boolean support(Requirements requirements, Business business, Account account) {
    if (applicationRequireAdditionalCheck(account, requirements)) {
      // when additional checks are required,
      // we will move business onboarding status depending on the stripe required information
      return (!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
              && requirements.getCurrentlyDue().stream()
                  .anyMatch(
                      s ->
                          s.endsWith(DOCUMENT)
                              || s.endsWith(COMPANY_TAX_ID)
                              || s.endsWith(SSN_LAST_4)))
          || (!CollectionUtils.isEmpty(requirements.getPastDue())
              && requirements.getPastDue().stream()
                  .anyMatch(
                      s ->
                          s.endsWith(DOCUMENT)
                              || s.endsWith(COMPANY_TAX_ID)
                              || s.endsWith(SSN_LAST_4)))
          || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
              && requirements.getEventuallyDue().stream()
                  .anyMatch(
                      s ->
                          s.endsWith(DOCUMENT)
                              || s.endsWith(COMPANY_TAX_ID)
                              || s.endsWith(SSN_LAST_4)));
    }
    return false;
  }

  @Override
  @TwilioKycKybOp(
      reviewer = "Craig Miller",
      explanation =
          "The KYB/KYC logic happens within onboarding and cannot enforce user permissions")
  public List<String> execute(
      Requirements requirements, Business business, Account account, boolean sendEmail) {
    if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.SOFT_FAIL)) {
      updateBusiness(
          business.getId(), null, BusinessOnboardingStep.SOFT_FAIL, KnowYourBusinessStatus.REVIEW);
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
      if (sendEmail) {
        twilioService.sendKybKycRequireDocumentsEmail(
            business.getBusinessEmail().getEncrypted(),
            businessOwner.getFirstName().getEncrypted(),
            reasons);
      }
      return reasons;
    }
    return extractErrorMessages(requirements);
  }
}
