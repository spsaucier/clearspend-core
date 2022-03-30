package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.common.error.InvalidKycStepException;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.StripeRequirements;
import com.clearspend.capital.data.repository.business.StripeRequirementsRepository;
import com.clearspend.capital.service.ApplicationReviewService;
import com.clearspend.capital.service.BusinessOwnerService;
import com.stripe.model.Account.Requirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessKycStepHandler {

  private final List<BusinessKycStep> steps;
  private final StripeRequirementsRepository stripeRequirementsRepository;
  private final BusinessOwnerService businessOwnerService;
  private final ApplicationReviewService applicationReviewService;

  @Transactional
  public List<String> execute(Business business, Account account) {
    log.info("Execute update account event for businessId {}.", business.getBusinessId());
    Requirements requirements = account.getRequirements();

    // This is for testing case, in real case we should not have null requirements
    if (requirements == null) {
      log.error(
          "Null requirements for businessId {} and stripeId {}.",
          business.getBusinessId(),
          account.getId());
      return new ArrayList<>();
    }

    boolean sendEmailForRequirements = false;
    Optional<StripeRequirements> stripeRequirementsOptional =
        stripeRequirementsRepository.findByBusinessId(business.getId());
    if (stripeRequirementsOptional.isPresent()) {
      StripeRequirements entityStripeRequirements = stripeRequirementsOptional.get();
      List<BusinessOwner> businessOwners =
          businessOwnerService.findBusinessOwnerByBusinessId(business.getId());
      if (!applicationReviewService
          .getReviewRequirements(business, businessOwners, requirements)
          .equals(
              applicationReviewService.getReviewRequirements(
                  business, businessOwners, entityStripeRequirements.getRequirements()))) {
        if (!steps
            .get(0)
            .extractErrorMessages(requirements)
            .equals(
                steps.get(0).extractErrorMessages(entityStripeRequirements.getRequirements()))) {
          sendEmailForRequirements = true;
        }
        entityStripeRequirements.setRequirements(requirements);
        stripeRequirementsRepository.save(entityStripeRequirements);
      }
    } else {
      stripeRequirementsRepository.save(new StripeRequirements(business.getId(), requirements));
      sendEmailForRequirements = true;
    }

    List<String> errorList =
        steps.stream()
            .filter(businessKycStep -> businessKycStep.support(requirements, business, account))
            .findFirst()
            .orElseThrow(InvalidKycStepException::new)
            .execute(requirements, business, account, sendEmailForRequirements);
    log.info(
        "Send email {} for stripe status update, error list. {}",
        sendEmailForRequirements,
        errorList);
    log.info(
        "Current onboarding step for businessId {} is {}",
        business.getBusinessId(),
        business.getOnboardingStep());
    return errorList;
  }
}
