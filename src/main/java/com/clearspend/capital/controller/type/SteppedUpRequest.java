package com.clearspend.capital.controller.type;

public interface SteppedUpRequest<T extends SteppedUpRequest<T>> {

  String getTrustChallenge();

  String getTwoFactorId();

  String getTwoFactorCode();

  T withTwoFactorParams(
      final String trustChallenge, final String twoFactorId, final String twoFactorCode);
}
