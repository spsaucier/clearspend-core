package com.clearspend.capital.controller.type.review;

public enum StripeAccountDisabledReason {
  requirementsPastDue("requirements.past_due"),
  requirements_pending_verification("requirements.pending_verification"),
  listed("listed"),
  platform_paused("platform_paused"),
  rejected_fraud("rejected.fraud"),
  rejected_listed("rejected.listed"),
  rejected_terms_of_service("rejected.terms_of_service"),
  rejected_other("rejected.other"),
  under_review("under_review"),
  other("other");

  String value_code;

  StripeAccountDisabledReason(String value_code) {
    this.value_code = value_code;
  }
}
