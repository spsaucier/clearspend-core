package com.clearspend.capital.common.error;

public class InvalidRequestException extends RuntimeException {

  public InvalidRequestException(String message) {
    super(message);
  }

  public InvalidRequestException(String message, Throwable exception) {
    super(message, exception);
  }
}
