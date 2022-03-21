package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.data.model.User;

public record CustomUser(User user, ValidationResult result) {
  public enum ValidationResult {
    PASS,
    FAIL
  }

  public static CustomUser pass(final User user) {
    return new CustomUser(user, ValidationResult.PASS);
  }

  public static CustomUser fail(final User user) {
    return new CustomUser(user, ValidationResult.FAIL);
  }

  public boolean isPassResult() {
    return result == ValidationResult.PASS;
  }
}
