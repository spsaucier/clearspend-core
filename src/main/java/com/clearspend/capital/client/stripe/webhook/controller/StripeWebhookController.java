package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeProperties;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.StripeWebhookLog;
import com.clearspend.capital.data.repository.StripeWebhookLogRepository;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Transaction;
import com.stripe.net.Webhook;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stripe/webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

  private final StripeWebhookLogRepository stripeWebhookLogRepository;

  private final StripeProperties stripeProperties;

  private final NetworkMessageService networkMessageService;

  @PostMapping("")
  private void webhook(HttpServletRequest request) {
    Instant start = Instant.now();

    // FIXME(kuchlein): need to encrypt the payload or simply drop the table. Kept like this to make
    //  debugging easier
    // record the data we get from Stripe into the database
    StripeWebhookLog stripeWebhookLog = new StripeWebhookLog();

    String sigHeader = request.getHeader("Stripe-Signature");
    Event event;
    try {
      String payload = IOUtils.toString(request.getReader());
      stripeWebhookLog.setRequest(payload);
      log.info("payload: {}", payload);
      event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getSecret());
      stripeWebhookLog.setEventType(event.getType());
    } catch (IOException e) {
      stripeWebhookLog.setError(e.getMessage());
      e.printStackTrace();
      throw new InvalidRequestException("Failed to read body: " + e.getMessage());
    } catch (SignatureVerificationException e) {
      stripeWebhookLog.setError(e.getMessage());
      e.printStackTrace();
      throw new InvalidRequestException("Invalid signature: " + e.getMessage());
    } finally {
      // no matter what happens try to save the request into the database
      stripeWebhookLog = stripeWebhookLogRepository.save(stripeWebhookLog);
    }

    // Deserialize the nested object inside the event
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
    if (dataObjectDeserializer.getObject().isEmpty()) {
      // Deserialization failed, probably due to an API version mismatch.
      // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
      // instructions on how to handle this case, or return an error here.
      throw new InvalidRequestException("failed to deserialize Stripe request");
    }

    // determine the event type and handle it
    StripeEventType stripeEventType = StripeEventType.fromString(event.getType());
    try {
      switch (stripeEventType) {
        case ISSUING_AUTHORIZATION_REQUEST, ISSUING_AUTHORIZATION_CREATED -> processPayment(
            stripeEventType, dataObjectDeserializer);
        case ISSUING_TRANSACTION_CREATED -> processCompletion(
            stripeEventType, dataObjectDeserializer);
        case ISSUING_CARD_CREATED -> processCard(stripeEventType, dataObjectDeserializer);
        case ISSUING_CARDHOLDER_CREATED, ISSUING_CARDHOLDER_UPDATED -> processCardHolder(
            stripeEventType, dataObjectDeserializer);
        default -> {
          String msg = "unsupported eventType: " + event.getType();
          log.error(msg);
          throw new InvalidRequestException(msg);
        }
      }
    } catch (StripeException e) {
      e.printStackTrace();
    }

    // capture total processing time
    Instant end = Instant.now();
    stripeWebhookLog.setProcessingTimeMs(Duration.between(start, end).toMillis());
    stripeWebhookLogRepository.save(stripeWebhookLog);
  }

  private void processCompletion(
      StripeEventType stripeEventType, EventDataObjectDeserializer dataObjectDeserializer) {
    Transaction transaction = (Transaction) dataObjectDeserializer.getObject().get();
    NetworkCommon common = new NetworkCommon(transaction, dataObjectDeserializer.getRawJson());
    NetworkMessage networkMessage = networkMessageService.processNetworkMessage(common);
  }

  private void processPayment(
      StripeEventType stripeEventType, EventDataObjectDeserializer dataObjectDeserializer)
      throws StripeException {
    switch (stripeEventType) {
      case ISSUING_AUTHORIZATION_REQUEST -> {
        Authorization auth = (Authorization) dataObjectDeserializer.getObject().get();
        if (auth.getStatus() != "pending") {
          // TODO(kuchlein): handle "closed" and "reversed" cases
        }
        NetworkCommon common = new NetworkCommon(auth, dataObjectDeserializer.getRawJson());
        NetworkMessage networkMessage = networkMessageService.processNetworkMessage(common);

        if (common.isPostDecline()) {
          auth.decline();
        } else {
          auth.approve();
        }
      }
      case ISSUING_AUTHORIZATION_CREATED -> {
        Authorization auth = (Authorization) dataObjectDeserializer.getObject().get();
      }
    }
  }

  private void processCard(
      StripeEventType stripeEventType, EventDataObjectDeserializer dataObjectDeserializer)
      throws StripeException {}

  private void processCardHolder(
      StripeEventType stripeEventType, EventDataObjectDeserializer dataObjectDeserializer)
      throws StripeException {}
}
