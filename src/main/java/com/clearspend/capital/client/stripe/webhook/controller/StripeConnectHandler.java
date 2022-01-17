package com.clearspend.capital.client.stripe.webhook.controller;

import com.stripe.model.StripeObject;
import org.springframework.stereotype.Component;

@Component
public class StripeConnectHandler {

  public void accountUpdated(StripeObject stripeObject) {}

  public void externalAccountCreated(StripeObject stripeObject) {}

  public void inboundTransferCreated(StripeObject stripeObject) {}

  public void inboundTransferSucceeded(StripeObject stripeObject) {}

  public void inboundTransferFailed(StripeObject stripeObject) {}

  public void outboundTransferCreated(StripeObject stripeObject) {}

  public void outboundTransferReturned(StripeObject stripeObject) {}

  public void outboundTransferFailed(StripeObject stripeObject) {}

  public void outboundTransferCancelled(StripeObject stripeObject) {}

  public void outboundTransferExpectedArrivalDateUpdated(StripeObject stripeObject) {}

  public void outboundTransferPosted(StripeObject stripeObject) {}

  public void outboundPaymentCreated(StripeObject stripeObject) {}

  public void outboundPaymentProccessing(StripeObject stripeObject) {}

  public void outboundPaymentCancelled(StripeObject stripeObject) {}

  public void outboundPaymentFailed(StripeObject stripeObject) {}

  public void outboundPaymentExpectedArrivalDateUpdated(StripeObject stripeObject) {}

  public void outboundPaymentPosted(StripeObject stripeObject) {}

  public void outboundPaymentReturned(StripeObject stripeObject) {}
}
