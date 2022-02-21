package com.clearspend.capital.data.model.enums;

import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/*
null (initial) -> BUSINESS_OWNERS
BUSINESS_OWNERS -> LINK_ACCOUNT
BUSINESS_OWNERS -> REVIEW
BUSINESS_OWNERS -> SOFT_FAIL
SOFT_FAIL -> REVIEW
SOFT_FAIL -> BUSINESS
SOFT_FAIL -> BUSINESS_OWNER
SOFT_FAIL -> LINK_ACCOUNT
SOFT_FAIL -> SOFT_FAIL
REVIEW -> SOFT_FAIL
REVIEW -> BUSINESS
REVIEW -> BUSINESS_OWNER
REVIEW -> LINK_ACCOUNT
LINK_ACCOUNT -> TRANSFER_MONEY
TRANSFER_MONEY -> COMPLETE (terminal)
 */
@RequiredArgsConstructor
public enum BusinessOnboardingStep {
  BUSINESS,
  BUSINESS_OWNERS,
  SOFT_FAIL,
  REVIEW,
  LINK_ACCOUNT,
  TRANSFER_MONEY,
  COMPLETE;

  private static final Map<BusinessOnboardingStep, Set<BusinessOnboardingStep>> stateTransfers =
      Map.of(
          BUSINESS, Set.of(BUSINESS_OWNERS),
          BUSINESS_OWNERS, Set.of(LINK_ACCOUNT, REVIEW, SOFT_FAIL, BUSINESS),
          SOFT_FAIL, Set.of(REVIEW, BUSINESS, BUSINESS_OWNERS, LINK_ACCOUNT, SOFT_FAIL),
          REVIEW, Set.of(SOFT_FAIL, BUSINESS, BUSINESS_OWNERS, LINK_ACCOUNT),
          LINK_ACCOUNT, Set.of(TRANSFER_MONEY, COMPLETE),
          TRANSFER_MONEY, Set.of(COMPLETE),
          COMPLETE, Set.of());

  public boolean canTransferTo(BusinessOnboardingStep businessOnboardingStep) {
    return stateTransfers.get(this).contains(businessOnboardingStep);
  }
}
