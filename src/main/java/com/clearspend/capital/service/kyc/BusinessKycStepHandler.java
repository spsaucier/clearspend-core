package com.clearspend.capital.service.kyc;

import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.data.model.business.Business;
import com.stripe.model.Account.Requirements;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessKycStepHandler {

  private final List<BusinessKycStep> steps;

  public List<String> execute(Business business, Account account) {

    Requirements requirements = account.getRequirements();

    // This is for testing case, in real case we should not have null requirements
    if (requirements == null) {
      return new ArrayList<>();
    }

    // TODO : gb: save requirements and check if email should be send for new requirements

    return steps.stream()
        .filter(businessKycStep -> businessKycStep.support(requirements, business, account))
        .findFirst()
        .orElseThrow(() -> new RuntimeException(""))
        .execute(requirements, business, account);
  }
}
