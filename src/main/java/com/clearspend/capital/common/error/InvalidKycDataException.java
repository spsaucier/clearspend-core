package com.clearspend.capital.common.error;

public class InvalidKycDataException extends RuntimeException {

  public <T extends Enum<T>> InvalidKycDataException(String identifier, String cause) {
    super(String.format("Invalid data for %s. %s", identifier, cause));
  }

  public <T extends Enum<T>> InvalidKycDataException(String cause) {
    super(String.format("Invalid data. %s", cause));
  }
}
