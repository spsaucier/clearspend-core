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
@Order(3)
public class BusinessKycStep3LinkAccount extends BusinessKycStep {

  @Override
  public boolean support(Requirements requirements, Business business, Account account) {

    return Boolean.FALSE.equals(applicationRequireAdditionalCheck(account, requirements));
  }

  @Override
  @TwilioKycKybOp(
      reviewer = "Craig Miller",
      explanation =
          "The KYB/KYC logic happens within onboarding and cannot enforce user permissions")
  public List<String> execute(
      Requirements requirements, Business business, Account account, boolean sendEmail) {

    if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.LINK_ACCOUNT)) {
      updateBusiness(
          business.getId(), null, BusinessOnboardingStep.LINK_ACCOUNT, KnowYourBusinessStatus.PASS);
      BusinessOwner businessOwner =
          businessOwnerRepository
              .findByBusinessIdAndEmailHash(
                  business.getId(),
                  HashUtil.calculateHash(business.getBusinessEmail().getEncrypted()))
              .orElse(
                  businessOwnerRepository.findByBusinessId(business.getId()).stream()
                      .findAny()
                      .orElseThrow());

      if (sendEmail) {
        twilioService.sendKybKycPassEmail(
            business.getBusinessEmail().getEncrypted(),
            businessOwner.getFirstName().getEncrypted());
      }
    }

    return extractErrorMessages(requirements);
  }
}
