package com.clearspend.capital.common.error;

public class StripeAccountDocumentUpdateException extends RuntimeException {

  public StripeAccountDocumentUpdateException(String message) {
    super(message);
  }

  public StripeAccountDocumentUpdateException(String message, Throwable exception) {
    super(message, exception);
  }
}
