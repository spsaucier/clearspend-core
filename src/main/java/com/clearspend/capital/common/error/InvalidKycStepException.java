package com.clearspend.capital.common.error;

public class InvalidKycStepException extends RuntimeException {

  public <T extends Enum<T>> InvalidKycStepException() {
    super(String.format("Invalid kyc step exception."));
  }
}
