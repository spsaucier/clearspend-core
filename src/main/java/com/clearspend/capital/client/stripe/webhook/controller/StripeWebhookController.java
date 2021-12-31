package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeProperties;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.StripeWebhookLog;
import com.clearspend.capital.data.repository.StripeWebhookLogRepository;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.google.common.annotations.VisibleForTesting;
import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Transaction;
import com.stripe.net.Webhook;
import com.stripe.param.issuing.AuthorizationApproveParams;
import com.stripe.param.issuing.AuthorizationApproveParams.Builder;
import com.stripe.param.issuing.AuthorizationDeclineParams;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
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

    ParseRecord parseRecord = parseRequest("connect", request, stripeProperties.getConnectSecret());

    switch (parseRecord.stripeEventType) {
      case ACCOUNT_UPDATED -> accountUpdated(parseRecord.stripeEventType, parseRecord.stripeObject);
      case ACCOUNT_EXTERNAL_ACCOUNT_CREATED -> externalAccountCreated(
          parseRecord.stripeEventType, parseRecord.stripeObject);
      default -> {
        String errorMessage = "unhandled eventType: " + parseRecord.stripeWebhookLog.getEventType();
        log.error(errorMessage);
        parseRecord.stripeWebhookLog.setError(errorMessage);
      }
    }

    // capture total processing time or -1 if an error occurred
    Instant end = Instant.now();
    parseRecord.stripeWebhookLog.setProcessingTimeMs(
        parseRecord.stripeWebhookLog.getError() != null
            ? Duration.between(start, end).toMillis()
            : -1);
    stripeWebhookLogRepository.save(parseRecord.stripeWebhookLog);
  }

  private void accountUpdated(StripeEventType stripeEventType, StripeObject stripeObject) {}

  private void externalAccountCreated(StripeEventType stripeEventType, StripeObject stripeObject) {}

  @PostMapping("/webhook/direct")
  private void directWebhook(HttpServletRequest request) {
    Instant start = Instant.now();

    ParseRecord parseRecord = parseRequest("direct", request, stripeProperties.getDirectSecret());

    try {
      switch (parseRecord.stripeEventType) {
        case ISSUING_AUTHORIZATION_REQUEST, ISSUING_AUTHORIZATION_CREATED -> processAuthorization(
            parseRecord.stripeEventType, parseRecord.stripeObject, parseRecord.rawJson);
        case ISSUING_TRANSACTION_CREATED -> processCompletion(
            parseRecord.stripeEventType, parseRecord.stripeObject, parseRecord.rawJson);
        case ISSUING_CARD_CREATED -> processCard(
            parseRecord.stripeEventType, parseRecord.stripeObject);
        case ISSUING_CARDHOLDER_CREATED, ISSUING_CARDHOLDER_UPDATED -> processCardHolder(
            parseRecord.stripeEventType, parseRecord.stripeObject);
        default -> {
          String errorMessage =
              "unhandled eventType: " + parseRecord.stripeWebhookLog.getEventType();
          log.error(errorMessage);
          parseRecord.stripeWebhookLog.setError(errorMessage);
        }
      }
    } catch (StripeException e) {
      e.printStackTrace();
    }

    // capture total processing time or -1 if an error occurred
    Instant end = Instant.now();
    parseRecord.stripeWebhookLog.setProcessingTimeMs(
        parseRecord.stripeWebhookLog.getError() != null
            ? Duration.between(start, end).toMillis()
            : -1);
    stripeWebhookLogRepository.save(parseRecord.stripeWebhookLog);
  }

  private record ParseRecord(
      StripeWebhookLog stripeWebhookLog,
      StripeObject stripeObject,
      String rawJson,
      StripeEventType stripeEventType) {}

  private ParseRecord parseRequest(String requestType, HttpServletRequest request, String secret) {
    // FIXME(kuchlein): need to encrypt the payload or simply drop the table. Kept like this to make
    //  debugging easier
    // record the data we get from Stripe into the database
    StripeWebhookLog stripeWebhookLog = new StripeWebhookLog();

    String sigHeader = request.getHeader("Stripe-Signature");
    Event event;
    String payload;
    try {
      payload = IOUtils.toString(request.getReader());
      stripeWebhookLog.setRequest(payload);
      log.info("{} payload: {}", requestType, payload);
      event = Webhook.constructEvent(payload, sigHeader, secret);
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

    // ensure that we have a supported API version request
    if (!event.getApiVersion().startsWith(Stripe.API_VERSION)) {
      throw new InvalidRequestException(
          String.format(
              "Unsupported Stripe API version %s, for event data object for %s",
              event.getApiVersion(), event.getId()));
    }

    // Deserialize the nested object inside the event
    StripeObject stripeObject;
    try {
      stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
    } catch (EventDataObjectDeserializationException e) {
      throw new InvalidRequestException("failed to deserialize Stripe request");
    }

    StripeEventType stripeEventType = StripeEventType.fromString(stripeWebhookLog.getEventType());

    return new ParseRecord(stripeWebhookLog, stripeObject, payload, stripeEventType);
  }

  @VisibleForTesting
  void processAuthorization(
      StripeEventType stripeEventType, StripeObject stripeObject, String rawJson)
      throws StripeException {
    switch (stripeEventType) {
      case ISSUING_AUTHORIZATION_REQUEST -> {
        Authorization auth = (Authorization) stripeObject;
        if (auth.getStatus() != "pending") {
          // TODO(kuchlein): handle "closed" and "reversed" cases
        }
        NetworkCommon common = new NetworkCommon(auth, rawJson);
        common.getRequestedAmount().ensureNonNegative();
        NetworkMessage networkMessage = networkMessageService.processNetworkMessage(common);

        Map<String, String> metadata = getMetadata(common, networkMessage);

        if (common.isPostDecline()) {
          AuthorizationDeclineParams authorizationDeclineParams =
              AuthorizationDeclineParams.builder().setMetadata(metadata).build();
          log.debug(
              "Stripe authorization {} for {} declined in {} for {}",
              auth.getId(),
              common.getRequestedAmount(),
              networkMessage != null ? networkMessage.getId() : "n/a",
              common.getRequestedAmount());
          auth.decline(authorizationDeclineParams);
        } else {
          auth.setApproved(true);
          Builder authorizationApproveParams =
              AuthorizationApproveParams.builder().setMetadata(metadata);
          if (common.isAllowPartialApproval()) {
            authorizationApproveParams.setAmount(common.getApprovedAmount().toStripeAmount());
          }
          log.debug(
              "Stripe authorization {} for {} approved in {} for {}",
              auth.getId(),
              common.getRequestedAmount(),
              networkMessage.getId(),
              common.getRequestedAmount());
          auth.approve(authorizationApproveParams.build());
        }
      }
      case ISSUING_AUTHORIZATION_CREATED -> {
        Authorization auth = (Authorization) stripeObject;
      }
    }
  }

  private void processCompletion(
      StripeEventType stripeEventType, StripeObject stripeObject, String rawJson) {
    Transaction transaction = (Transaction) stripeObject;
    NetworkCommon common = new NetworkCommon(transaction, rawJson);
    NetworkMessage networkMessage = networkMessageService.processNetworkMessage(common);
  }

  private void processCard(StripeEventType stripeEventType, StripeObject stripeObject) {}

  private void processCardHolder(StripeEventType stripeEventType, StripeObject stripeObject) {}

  private Map<String, String> getMetadata(NetworkCommon common, NetworkMessage networkMessage) {
    Map<String, String> metadata = new HashMap<>();

    if (common.getBusinessId() != null) {
      metadata.put("businessId", common.getBusinessId().toString());
    }
    if (common.getAllocation() != null && common.getAllocation().getId() != null) {
      metadata.put("allocationId", common.getAllocation().getId().toString());
    }
    if (common.getCard() != null && common.getCard().getId() != null) {
      metadata.put("cardId", common.getCard().getId().toString());
    }
    if (common.getAllocation() != null && common.getAllocation().getId() != null) {
      metadata.put("accountId", common.getAllocation().getId().toString());
    }
    if (common.getDeclineReasons() != null && common.getDeclineReasons().size() > 0) {
      metadata.put(
          "declineReasons",
          common.getDeclineReasons().stream().map(Enum::toString).collect(Collectors.joining(",")));
    }

    if (networkMessage != null) {
      if (networkMessage.getId() != null) {
        metadata.put("networkMessageId", networkMessage.getId().toString());
      }
      if (networkMessage.getAdjustmentId() != null) {
        metadata.put("adjustmentId", networkMessage.getAdjustmentId().toString());
      }
      if (networkMessage.getHoldId() != null) {
        metadata.put("holdId", networkMessage.getHoldId().toString());
      }
    }

    return metadata;
  }
}
