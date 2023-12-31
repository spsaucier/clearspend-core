package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeClientException;
import com.clearspend.capital.client.stripe.StripeProperties;
import com.clearspend.capital.common.advice.AssignApplicationSecurityContextAdvice.SecureWebhook;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.data.repository.network.StripeWebhookLogRepository;
import com.clearspend.capital.service.DistributedLockerService;
import com.clearspend.capital.service.NetworkMessageEnrichmentService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.google.common.annotations.VisibleForTesting;
import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.HasId;
import com.stripe.model.StripeObject;
import com.stripe.model.issuing.Transaction;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/stripe")
@Slf4j
@SecureWebhook
public class StripeWebhookController {

  private static final EnumSet<StripeEventType> AUTH_EVENT_TYPES =
      EnumSet.of(
          StripeEventType.ISSUING_AUTHORIZATION_REQUEST,
          StripeEventType.ISSUING_AUTHORIZATION_CREATED,
          StripeEventType.ISSUING_AUTHORIZATION_UPDATED,
          StripeEventType.ISSUING_TRANSACTION_CREATED);

  private final NetworkMessageEnrichmentService networkMessageEnrichmentService;
  private final StripeWebhookLogRepository stripeWebhookLogRepository;
  private final StripeProperties stripeProperties;
  private final StripeConnectHandler stripeConnectHandler;
  private final StripeDirectHandler stripeDirectHandler;
  private final WebClient authFallbackClient;
  private final DistributedLockerService distributedLockerService;

  public StripeWebhookController(
      StripeWebhookLogRepository stripeWebhookLogRepository,
      StripeProperties stripeProperties,
      StripeConnectHandler stripeConnectHandler,
      StripeDirectHandler stripeDirectHandler,
      NetworkMessageEnrichmentService networkMessageEnrichmentService,
      @Autowired(required = false) @Qualifier("authFallbackClient") WebClient authFallbackClient,
      DistributedLockerService distributedLockerService) {
    this.stripeWebhookLogRepository = stripeWebhookLogRepository;
    this.stripeProperties = stripeProperties;
    this.stripeConnectHandler = stripeConnectHandler;
    this.stripeDirectHandler = stripeDirectHandler;
    this.authFallbackClient = authFallbackClient;
    this.networkMessageEnrichmentService = networkMessageEnrichmentService;
    this.distributedLockerService = distributedLockerService;
  }

  @PostMapping("/webhook/connect")
  void connectWebhook(HttpServletRequest request) {
    Instant instant = Instant.now();
    ParseRecord parseRecord = parseRequest("connect", request, stripeProperties.getConnectSecret());

    executeWithObjectLock(
        parseRecord.stripeObject(), () -> handleConnectRequest(instant, parseRecord));
  }

  void handleConnectRequest(Instant start, ParseRecord parseRecord) {
    StripeObject stripeObject = parseRecord.stripeObject();

    switch (parseRecord.stripeEventType) {
        // inbound transfers
      case INBOUND_TRANSFER_CREATED -> stripeConnectHandler.inboundTransferCreated(stripeObject);
      case INBOUND_TRANSFER_SUCCEEDED -> stripeConnectHandler.inboundTransferSucceeded(
          stripeObject);
      case INBOUND_TRANSFER_FAILED -> stripeConnectHandler.inboundTransferFailed(stripeObject);

      case RECEIVED_CREDIT_CREATED -> stripeConnectHandler.receivedCreditCreated(stripeObject);

        // outbound transfers
      case OUTBOUND_TRANSFER_CREATED -> stripeConnectHandler.outboundTransferCreated(stripeObject);
      case OUTBOUND_TRANSFER_RETURNED -> stripeConnectHandler.outboundTransferReturned(
          stripeObject);
      case OUTBOUND_TRANSFER_FAILED -> stripeConnectHandler.outboundTransferFailed(stripeObject);
      case OUTBOUND_TRANSFER_CANCELED -> stripeConnectHandler.outboundTransferCancelled(
          stripeObject);
      case OUTBOUND_TRANSFER_EXPECTED_ARRIVAL_DATE_UPDATED -> stripeConnectHandler
          .outboundTransferExpectedArrivalDateUpdated(stripeObject);
      case OUTBOUND_TRANSFER_POSTED -> stripeConnectHandler.outboundTransferPosted(stripeObject);

        // outbound payments
      case OUTBOUND_PAYMENT_CREATED -> stripeConnectHandler.outboundPaymentCreated(stripeObject);
      case OUTBOUND_PAYMENT_PROCESSING -> stripeConnectHandler.outboundPaymentProccessing(
          stripeObject);
      case OUTBOUND_PAYMENT_CANCELED -> stripeConnectHandler.outboundPaymentCancelled(stripeObject);
      case OUTBOUND_PAYMENT_FAILED -> stripeConnectHandler.outboundPaymentFailed(stripeObject);
      case OUTBOUND_PAYMENT_EXPECTED_ARRIVAL_DATE_UPDATED -> stripeConnectHandler
          .outboundPaymentExpectedArrivalDateUpdated(stripeObject);
      case OUTBOUND_PAYMENT_POSTED -> stripeConnectHandler.outboundPaymentPosted(stripeObject);
      case OUTBOUND_PAYMENT_RETURNED -> stripeConnectHandler.outboundPaymentReturned(stripeObject);

        // connected accounts
      case ACCOUNT_UPDATED -> stripeConnectHandler.accountUpdated(
          parseRecord.stripeEventObject, (Account) stripeObject);
      case ACCOUNT_EXTERNAL_ACCOUNT_CREATED -> stripeConnectHandler.externalAccountCreated(
          stripeObject);
      case FINANCIAL_ACCOUNT_FEATURES_STATUS_UPDATED -> stripeConnectHandler
          .financialAccountFeaturesUpdated(stripeObject);

      default -> {
        String errorMessage = "unhandled eventType: " + parseRecord.stripeWebhookLog.getEventType();
        log.warn(errorMessage);
        parseRecord.stripeWebhookLog.setError(errorMessage);
      }
    }

    // capture total processing time or -1 if an error occurred
    parseRecord.stripeWebhookLog.setProcessingTimeMs(
        Duration.between(start, Instant.now()).toMillis());
    stripeWebhookLogRepository.save(parseRecord.stripeWebhookLog);
  }

  @PostMapping("/webhook/issuing")
  void directWebhook(HttpServletRequest request) {
    Instant instant = Instant.now();
    ParseRecord parseRecord = parseRequest("issuing", request, stripeProperties.getIssuingSecret());

    executeWithObjectLock(
        parseRecord.stripeObject(), () -> handleDirectRequest(instant, parseRecord));
  }

  @VisibleForTesting
  @SuppressWarnings("CatchAndPrintStackTrace")
  NetworkCommon handleDirectRequest(Instant start, ParseRecord parseRecord) {
    NetworkCommon networkCommon = null;
    try {
      switch (parseRecord.stripeEventType) {
        case ISSUING_AUTHORIZATION_REQUEST,
            ISSUING_AUTHORIZATION_CREATED,
            ISSUING_AUTHORIZATION_UPDATED -> networkCommon =
            stripeDirectHandler.processAuthorization(parseRecord);
        case ISSUING_TRANSACTION_CREATED -> networkCommon =
            stripeDirectHandler.processCapture(parseRecord);
        case ISSUING_CARD_CREATED, ISSUING_CARD_UPDATED -> stripeDirectHandler.processCard(
            parseRecord.stripeEventType, parseRecord);
        case ISSUING_CARDHOLDER_CREATED, ISSUING_CARDHOLDER_UPDATED -> stripeDirectHandler
            .processCardHolder(parseRecord.stripeEventType, parseRecord.stripeObject);
        default -> {
          String errorMessage =
              "unhandled eventType: " + parseRecord.stripeWebhookLog.getEventType();
          log.error(errorMessage);
          parseRecord.stripeWebhookLog.setError(errorMessage);
        }
      }
    } catch (StripeException | StripeClientException e) {
      log.error("Failed to process direct request", e);
    } catch (RecordNotFoundException e) {
      // For test webhooks we might want to delegate the auth request to another endpoint since
      // stripe can only deliver auth event to one instance only (unlike other events)
      if (e.getTable() == Table.CARD
          && authFallbackClient != null
          && stripeProperties.isTestMode()
          && AUTH_EVENT_TYPES.contains(parseRecord.stripeEventType)) {
        Mono<String> response =
            authFallbackClient
                .post()
                .bodyValue(parseRecord.stripeWebhookLog().getRequest())
                .retrieve()
                .bodyToMono(String.class);
        response.subscribe(
            r -> log.debug("Auth request has been delivered to fallback url"),
            t -> log.error("Failed to deliver auth request to fallback url", t));
      }
    }

    // capture total processing time or -1 if an error occurred
    parseRecord.stripeWebhookLog.setProcessingTimeMs(
        Duration.between(start, Instant.now()).toMillis());
    stripeWebhookLogRepository.save(parseRecord.stripeWebhookLog);

    // Schedule enrichment to run in another thread.
    if (networkCommon != null) {
      networkMessageEnrichmentService.scheduleActivityEnrichment(networkCommon);
    }

    return networkCommon;
  }

  record ParseRecord(
      StripeWebhookLog stripeWebhookLog,
      StripeObject stripeObject,
      StripeObject stripeEventObject,
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
      if (stripeProperties.isAllowSkipStripeHeaderValidation()
          && "true".equals(request.getHeader("skip-stripe-header-verification"))) {
        event = ApiResource.GSON.fromJson(payload, Event.class);
      } else {
        event = Webhook.constructEvent(payload, sigHeader, secret);
      }
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

    return new ParseRecord(stripeWebhookLog, stripeObject, event, stripeEventType);
  }

  /**
   * In order to prevent concurrent modification updates when we receive two stripe events for the
   * same object at the same time we need to block on the underlying object id
   */
  @VisibleForTesting
  void executeWithObjectLock(StripeObject stripeObject, Runnable runnable) {
    String objectId = null;

    if (stripeObject instanceof Transaction transaction) {
      // for captures we need to reference original authorization to avoid auth update/capture pairs
      objectId = transaction.getAuthorization();
    } else if (stripeObject instanceof HasId identifiableStripeObject) {
      // this mechanism only supports official events (non beta). If we ever see concurrent
      // modification with beta events we need to handle it separately
      objectId = identifiableStripeObject.getId();
    }

    if (objectId != null) {
      distributedLockerService.doWithLock(objectId, runnable);
    } else {
      runnable.run();
    }
  }
}
