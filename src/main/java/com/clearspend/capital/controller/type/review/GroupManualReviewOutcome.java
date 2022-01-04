package com.clearspend.capital.controller.type.review;

import java.util.Arrays;

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

  public static GroupManualReviewOutcome getEnumByValue(String value) {
    return Arrays.stream(GroupManualReviewOutcome.values())
        .filter(reviewOutcome -> reviewOutcome.value.equals(value))
        .findAny()
        .orElseThrow();
  }
}
