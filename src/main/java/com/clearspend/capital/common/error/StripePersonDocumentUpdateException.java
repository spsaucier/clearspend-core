package com.clearspend.capital.common.error;

public class StripePersonDocumentUpdateException extends RuntimeException {

  public StripePersonDocumentUpdateException(String message) {
    super(message);
  }

  public StripePersonDocumentUpdateException(String message, Throwable exception) {
    super(message, exception);
  }
}
