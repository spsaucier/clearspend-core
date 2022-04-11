package com.clearspend.capital.common.error;

import lombok.Getter;

@Getter
public class InvalidKycDataException extends RuntimeException {

  private final String param;

  public <T extends Enum<T>> InvalidKycDataException(String identifier, String cause) {
    super(String.format("Invalid data for %s. %s", identifier, cause));
    this.param = identifier;
  }

  public <T extends Enum<T>> InvalidKycDataException(String cause) {
    super(String.format("Invalid data. %s", cause));
    this.param = "";
  }
}
