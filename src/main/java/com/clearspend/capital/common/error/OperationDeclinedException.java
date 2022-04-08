package com.clearspend.capital.common.error;

/**
 * Base exception class for different exceptions that can block transactions/operations on our side
 * (limits, operation limits, spend controls, funds availability)
 */
public class OperationDeclinedException extends RuntimeException {

  public OperationDeclinedException(String message) {
    super(message);
  }
}
