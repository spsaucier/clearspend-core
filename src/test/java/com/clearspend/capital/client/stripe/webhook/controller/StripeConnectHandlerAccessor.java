package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.OutboundTransfer;
import com.clearspend.capital.client.stripe.types.ReceivedCredit;
import com.clearspend.capital.client.stripe.types.StripeNetwork;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StripeConnectHandlerAccessor {

  private final StripeConnectHandler stripeConnectHandler;

  public void processInboundTransferResult(InboundTransfer inboundTransfer) {
    stripeConnectHandler.processInboundTransferResult(inboundTransfer);
  }

  public void processOutboundTransferResult(OutboundTransfer outboundTransfer) {
    stripeConnectHandler.processOutboundTransferResult(outboundTransfer);
  }

  public void onAchCreditsReceived(ReceivedCredit receivedCredit, StripeNetwork stripeNetwork) {
    stripeConnectHandler.onAchCreditsReceived(receivedCredit, stripeNetwork);
  }

  public void onCardCreditsReceived(ReceivedCredit receivedCredit) {
    stripeConnectHandler.onCardCreditsReceived(receivedCredit);
  }
}
