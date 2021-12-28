package com.clearspend.capital.client.stripe;

import com.stripe.exception.StripeException;

public class StripeClientException extends RuntimeException {

  public StripeClientException(StripeException e) {
    super(e);
  }
}
