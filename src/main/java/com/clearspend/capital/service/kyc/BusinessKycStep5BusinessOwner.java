package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.data.model.business.Business;
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
@Order(5)
public class BusinessKycStep5BusinessOwner extends BusinessKycStep {

  @Override
  public boolean support(Requirements requirements, Business business, Account account) {

    if (applicationRequireAdditionalCheck(account, requirements)) {
      // when additional checks are required,
      // we will move business onboarding status depending on the stripe required information
      return (!CollectionUtils.isEmpty(requirements.getCurrentlyDue())
              && requirements.getCurrentlyDue().stream().anyMatch(this::personRequirementsMatch))
          || (!CollectionUtils.isEmpty(requirements.getPastDue())
              && requirements.getPastDue().stream().anyMatch(this::personRequirementsMatch))
          || (!CollectionUtils.isEmpty(requirements.getEventuallyDue())
              && requirements.getEventuallyDue().stream().anyMatch(this::personRequirementsMatch));
    }

    return false;
  }

  @Override
  public List<String> execute(
      Requirements requirements, Business business, Account account, boolean sendEmail) {
    if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.BUSINESS_OWNERS)) {
      updateBusiness(
          business.getId(),
          null,
          BusinessOnboardingStep.BUSINESS_OWNERS,
          KnowYourBusinessStatus.PENDING);
    }
    return extractErrorMessages(requirements);
  }
}
