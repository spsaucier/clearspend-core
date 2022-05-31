package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.stripe.webhook.controller.StripeWebhookController.ParseRecord;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.stripe.exception.StripeException;
import com.stripe.model.StripeObject;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Card;
import com.stripe.model.issuing.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripeDirectHandler {

  private final NetworkMessageService networkMessageService;
  private final CardService cardService;
  private final StripeClient stripeClient;

  NetworkCommon processAuthorization(ParseRecord parseRecord) throws StripeException {
    StripeEventType stripeEventType = parseRecord.stripeEventType();
    StripeObject stripeObject = parseRecord.stripeObject();

    NetworkCommon common;
    switch (stripeEventType) {
      case ISSUING_AUTHORIZATION_REQUEST -> {
        Authorization auth = (Authorization) stripeObject;
        if (!"pending".equals(auth.getStatus())) {
          // TODO(kuchlein): handle "closed" and "reversed" cases
        }
        common =
            new NetworkCommon(
                NetworkMessageType.AUTH_REQUEST, auth, parseRecord.stripeWebhookLog());
        networkMessageService.processNetworkMessage(common);

        if (common.isPostDecline()) {
          stripeClient.declineAuthorization(auth, common);
        } else {
          stripeClient.approveAuthorization(auth, common);
        }
      }
      case ISSUING_AUTHORIZATION_CREATED -> {
        Authorization auth = (Authorization) stripeObject;
        common =
            new NetworkCommon(
                NetworkMessageType.AUTH_CREATED, auth, parseRecord.stripeWebhookLog());
        networkMessageService.processNetworkMessage(common);
      }
      case ISSUING_AUTHORIZATION_UPDATED -> {
        Authorization auth = (Authorization) stripeObject;
        common =
            new NetworkCommon(
                NetworkMessageType.AUTH_UPDATED, auth, parseRecord.stripeWebhookLog());
        networkMessageService.processNetworkMessage(common);
      }
      default -> throw new RuntimeException("Unhandled Stripe event " + stripeEventType);
    }

    if (common.getNetworkMessage() != null) {
      parseRecord.stripeWebhookLog().setNetworkMessageId(common.getNetworkMessage().getId());
    }

    return common;
  }

  NetworkCommon processCapture(ParseRecord parseRecord) {
    Transaction transaction = (Transaction) parseRecord.stripeObject();
    NetworkCommon common = new NetworkCommon(transaction, parseRecord.stripeWebhookLog());
    networkMessageService.processNetworkMessage(common);

    if (common.getNetworkMessage() != null) {
      parseRecord.stripeWebhookLog().setNetworkMessageId(common.getNetworkMessage().getId());
    }

    return common;
  }

  void processCard(StripeEventType stripeEventType, ParseRecord parseRecord) {
    if (stripeEventType == StripeEventType.ISSUING_CARD_UPDATED) {
      Card card = (Card) parseRecord.stripeObject();
      if (card.getShipping() != null) {
        cardService.processCardShippingEvents(card);
      }
    }
  }

  void processCardHolder(StripeEventType stripeEventType, StripeObject stripeObject) {}
}
