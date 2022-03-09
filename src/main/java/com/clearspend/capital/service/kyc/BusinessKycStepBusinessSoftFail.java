package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
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
public class BusinessKycStepBusinessSoftFail extends BusinessKycStep {

  @Override
  public boolean support(Requirements requirements, Business business, Account account) {
    if (applicationRequireAdditionalCheck(account, requirements)) {
      // when additional checks are required,
      // we will move business onboarding status depending on the stripe required information
      if ((!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
              && requirements.getCurrentlyDue().stream()
                  .anyMatch(s -> s.endsWith(DOCUMENT) || s.endsWith(COMPANY_TAX_ID)))
          || (!CollectionUtils.isEmpty(requirements.getPastDue())
              && requirements.getPastDue().stream()
                  .anyMatch(s -> s.endsWith(DOCUMENT) || s.endsWith(COMPANY_TAX_ID)))
          || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
              && requirements.getEventuallyDue().stream()
                  .anyMatch(s -> s.endsWith(DOCUMENT) || s.endsWith(COMPANY_TAX_ID)))) {
        return business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.SOFT_FAIL);
      }
    }
    return false;
  }

  @Override
  public List<String> execute(Requirements requirements, Business business, Account account) {
    updateBusiness(
        business.getId(), null, BusinessOnboardingStep.SOFT_FAIL, KnowYourBusinessStatus.REVIEW);
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
}