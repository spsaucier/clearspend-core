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

@Service
@RequiredArgsConstructor
@Slf4j
@Order(7)
public class BusinessKycStep7BusinessReview extends BusinessKycStep {

  @Override
  public boolean support(Requirements requirements, Business business, Account account) {

    return applicationRequireAdditionalCheck(account, requirements);
  }

  @Override
  @TwilioKycKybOp(
      reviewer = "Craig Miller",
      explanation =
          "The KYB/KYC logic happens within onboarding and cannot enforce user permissions")
  public List<String> execute(
      Requirements requirements, Business business, Account account, boolean sendEmail) {
    if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.REVIEW)) {
      updateBusiness(
          business.getId(), null, BusinessOnboardingStep.REVIEW, KnowYourBusinessStatus.REVIEW);

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
        twilioService.sendKybKycReviewStateEmail(
            business.getBusinessEmail().getEncrypted(),
            businessOwner.getFirstName().getEncrypted());
      }

      return reasons;
    }

    return extractErrorMessages(requirements);
  }
}
