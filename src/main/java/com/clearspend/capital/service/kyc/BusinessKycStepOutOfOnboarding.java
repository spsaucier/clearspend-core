package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.stripe.model.Account.Requirements;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class BusinessKycStepOutOfOnboarding extends BusinessKycStep {

  @Override
  public boolean support(Requirements requirements, Business business, Account account) {
    return business.getStatus() != BusinessStatus.ONBOARDING
        || !List.of(
                BusinessOnboardingStep.BUSINESS,
                BusinessOnboardingStep.BUSINESS_OWNERS,
                BusinessOnboardingStep.REVIEW,
                BusinessOnboardingStep.SOFT_FAIL)
            .contains(business.getOnboardingStep());
  }

  @Override
  public List<String> execute(Requirements requirements, Business business, Account account) {

    if (Strings.isBlank(business.getStripeData().getFinancialAccountRef())) {
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
}
