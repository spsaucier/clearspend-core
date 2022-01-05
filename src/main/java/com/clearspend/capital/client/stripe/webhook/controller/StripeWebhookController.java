package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeProperties;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.data.model.StripeWebhookLog;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
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
    parseRecord.stripeWebhookLog.setProcessingTimeMs(
        Duration.between(start, Instant.now()).toMillis());
    stripeWebhookLogRepository.save(parseRecord.stripeWebhookLog);
  }

  private void accountUpdated(StripeEventType stripeEventType, StripeObject stripeObject) {}

  private void externalAccountCreated(StripeEventType stripeEventType, StripeObject stripeObject) {}

  @PostMapping("/webhook/direct")
  private void directWebhook(HttpServletRequest request) {
    handleDirectRequest(
        Instant.now(), parseRequest("direct", request, stripeProperties.getDirectSecret()), false);
  }

  @VisibleForTesting
  NetworkCommon handleDirectRequest(Instant start, ParseRecord parseRecord, boolean isTest) {
    NetworkCommon networkCommon = null;
    try {
      switch (parseRecord.stripeEventType) {
        case ISSUING_AUTHORIZATION_REQUEST,
            ISSUING_AUTHORIZATION_CREATED,
            ISSUING_AUTHORIZATION_UPDATED -> networkCommon =
            processAuthorization(parseRecord, isTest);
        case ISSUING_TRANSACTION_CREATED -> networkCommon = processCapture(parseRecord);
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
    parseRecord.stripeWebhookLog.setProcessingTimeMs(
        Duration.between(start, Instant.now()).toMillis());
    stripeWebhookLogRepository.save(parseRecord.stripeWebhookLog);

    return networkCommon;
  }

  record ParseRecord(
      StripeWebhookLog stripeWebhookLog,
      StripeObject stripeObject,
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
      log.info("{} payload: {}", requestType, payload.replaceAll("\n *", " "));
      event = Webhook.constructEvent(payload, sigHeader, secret);
      stripeWebhookLog.setStripeEventRef(event.getId());
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

    return new ParseRecord(stripeWebhookLog, stripeObject, stripeEventType);
  }

  private NetworkCommon processAuthorization(ParseRecord parseRecord, boolean isTest)
      throws StripeException {
    StripeEventType stripeEventType = parseRecord.stripeEventType;
    StripeObject stripeObject = parseRecord.stripeObject;

    NetworkCommon common = null;
    switch (stripeEventType) {
      case ISSUING_AUTHORIZATION_REQUEST -> {
        Authorization auth = (Authorization) stripeObject;
        if (auth.getStatus() != "pending") {
          // TODO(kuchlein): handle "closed" and "reversed" cases
        }
        common =
            new NetworkCommon(NetworkMessageType.AUTH_REQUEST, auth, parseRecord.stripeWebhookLog);
        networkMessageService.processNetworkMessage(common);

        Map<String, String> metadata = getMetadata(common);

        if (common.isPostDecline()) {
          AuthorizationDeclineParams authorizationDeclineParams =
              AuthorizationDeclineParams.builder().setMetadata(metadata).build();
          log.debug(
              "Stripe authorization {} for {} declined in {} for {}",
              auth.getId(),
              common.getRequestedAmount(),
              common.getNetworkMessage() != null ? common.getNetworkMessage().getId() : "n/a",
              common.getRequestedAmount());
          if (!isTest) {
            auth.decline(authorizationDeclineParams);
          }
        } else {
          auth.setApproved(true);
          Builder authorizationApproveParams =
              AuthorizationApproveParams.builder().setMetadata(metadata);
          if (common.isAllowPartialApproval()) {
            // amounts going back to Stripe for authorizations should be positive
            authorizationApproveParams.setAmount(common.getApprovedAmount().abs().toStripeAmount());
          }
          log.debug(
              "Stripe authorization {} for {} approved in {} for {}",
              auth.getId(),
              common.getRequestedAmount(),
              common.getNetworkMessage().getId(),
              // amounts going back to Stripe for authorizations should be positive
              common.getRequestedAmount().abs());
          if (!isTest) {
            auth.approve(authorizationApproveParams.build());
          }
        }
      }
      case ISSUING_AUTHORIZATION_CREATED -> {
        Authorization auth = (Authorization) stripeObject;
        common =
            new NetworkCommon(NetworkMessageType.AUTH_CREATED, auth, parseRecord.stripeWebhookLog);
        networkMessageService.processNetworkMessage(common);
      }
      case ISSUING_AUTHORIZATION_UPDATED -> {
        Authorization auth = (Authorization) stripeObject;
        common =
            new NetworkCommon(NetworkMessageType.AUTH_UPDATED, auth, parseRecord.stripeWebhookLog);
        networkMessageService.processNetworkMessage(common);
      }
    }

    if (common != null && common.getNetworkMessage() != null) {
      parseRecord.stripeWebhookLog().setNetworkMessageId(common.getNetworkMessage().getId());
    }

    return common;
  }

  private NetworkCommon processCapture(ParseRecord parseRecord) {
    Transaction transaction = (Transaction) parseRecord.stripeObject;
    NetworkCommon common = new NetworkCommon(transaction, parseRecord.stripeWebhookLog);
    networkMessageService.processNetworkMessage(common);

    if (common.getNetworkMessage() != null) {
      parseRecord.stripeWebhookLog().setNetworkMessageId(common.getNetworkMessage().getId());
    }

    return common;
  }

  private void processCard(StripeEventType stripeEventType, StripeObject stripeObject) {}

  private void processCardHolder(StripeEventType stripeEventType, StripeObject stripeObject) {}

  @VisibleForTesting
  Map<String, String> getMetadata(NetworkCommon common) {
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

    if (common.getNetworkMessage() != null) {
      if (common.getNetworkMessage().getId() != null) {
        metadata.put("networkMessageId", common.getNetworkMessage().getId().toString());
      }
      if (common.getNetworkMessage().getAdjustmentId() != null) {
        metadata.put("adjustmentId", common.getNetworkMessage().getAdjustmentId().toString());
      }
      if (common.getNetworkMessage().getHoldId() != null) {
        metadata.put("holdId", common.getNetworkMessage().getHoldId().toString());
      }
    }

    return metadata;
  }
}
