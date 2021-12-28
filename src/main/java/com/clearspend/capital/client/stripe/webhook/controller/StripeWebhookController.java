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
import com.stripe.param.issuing.AuthorizationApproveParams;
import com.stripe.param.issuing.AuthorizationDeclineParams;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

  private final StripeWebhookLogRepository stripeWebhookLogRepository;

  private final StripeProperties stripeProperties;

  private final NetworkMessageService networkMessageService;

  @PostMapping("/webhook/connect")
  private void connectWebhook(HttpServletRequest request) {
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
      log.info("connect payload: {}", payload);
      event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getConnectSecret());
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
      throw new InvalidRequestException(
          String.format("Unable to deserialize Stripe event data object for %s", event));
      // throw new InvalidRequestException("failed to deserialize Stripe request");
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
          String errorMessage = "unhandled eventType: " + event.getType();
          log.error(errorMessage);
          stripeWebhookLog.setError(errorMessage);
        }
      }
    } catch (StripeException e) {
      e.printStackTrace();
    }

    // capture total processing time or -1 if an error occurred
    Instant end = Instant.now();
    stripeWebhookLog.setProcessingTimeMs(
        stripeWebhookLog.getError() != null ? Duration.between(start, end).toMillis() : -1);
    stripeWebhookLogRepository.save(stripeWebhookLog);
  }

  @PostMapping("/webhook/direct")
  private void directWebhook(HttpServletRequest request) {
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
      log.info("direct payload: {}", payload);
      event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getDirectSecret());
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
      throw new InvalidRequestException(
          String.format("Unable to deserialize Stripe event data object for %s", event));
      // throw new InvalidRequestException("failed to deserialize Stripe request");
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
          String errorMessage = "unhandled eventType: " + event.getType();
          log.error(errorMessage);
          stripeWebhookLog.setError(errorMessage);
        }
      }
    } catch (StripeException e) {
      e.printStackTrace();
    }

    // capture total processing time or -1 if an error occurred
    Instant end = Instant.now();
    stripeWebhookLog.setProcessingTimeMs(
        stripeWebhookLog.getError() != null ? Duration.between(start, end).toMillis() : -1);
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

        Map<String, String> metadata = getMetadata(common, networkMessage);

        if (common.isPostDecline()) {
          AuthorizationDeclineParams authorizationDeclineParams =
              AuthorizationDeclineParams.builder().setMetadata(metadata).build();
          auth.decline(authorizationDeclineParams);
        } else {
          auth.setApproved(true);
          AuthorizationApproveParams authorizationApproveParams =
              AuthorizationApproveParams.builder()
                  .setAmount(common.getApprovedAmount().toStripeAmount())
                  .setMetadata(metadata)
                  .build();
          auth.approve(authorizationApproveParams);
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

  private Map<String, String> getMetadata(NetworkCommon common, NetworkMessage networkMessage) {
    Map<String, String> metadata = new HashMap<>();

    if (networkMessage.getId() != null) {
      metadata.put("networkMessageId", networkMessage.getId().toString());
    }
    if (common.getBusinessId() != null) {
      metadata.put("businessId", common.getBusinessId().toString());
    }
    if (common.getAllocation().getId() != null) {
      metadata.put("allocationId", common.getAllocation().getId().toString());
    }
    if (common.getCard() != null) {
      metadata.put("cardId", common.getCard().getId().toString());
    }
    if (common.getAllocation() != null) {
      metadata.put("accountId", common.getAllocation().getId().toString());
    }
    if (networkMessage.getAdjustmentId() != null) {
      metadata.put("adjustmentId", networkMessage.getAdjustmentId().toString());
    }
    if (networkMessage.getHoldId() != null) {
      metadata.put("holdId", networkMessage.getHoldId().toString());
    }

    return metadata;
  }
}
