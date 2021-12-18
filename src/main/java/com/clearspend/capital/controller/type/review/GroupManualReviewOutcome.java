package com.clearspend.capital.controller.type.review;

public enum GroupManualReviewOutcome {
  APPROVED("Approved"),
  DENIED("Denied"),
  RESUBMIT_DOCUMENT("Resubmit Document");

  String value;

  GroupManualReviewOutcome(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
