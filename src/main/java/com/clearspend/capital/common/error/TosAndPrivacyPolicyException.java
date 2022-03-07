package com.clearspend.capital.common.error;

public class TosAndPrivacyPolicyException extends RuntimeException {

  public TosAndPrivacyPolicyException() {
    super("Terms of Service and Privacy Policy must be accepted.");
  }
}
