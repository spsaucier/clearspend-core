package com.clearspend.capital.common.error;

import lombok.Getter;

@Getter
public class TwoFactorAuthenticationRequired extends RuntimeException {

  private final String twoFactorId;

  public TwoFactorAuthenticationRequired(String twoFactorId) {
    this.twoFactorId = twoFactorId;
  }
}
