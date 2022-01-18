package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.webhook.controller.StripeWebhookController.ParseRecord;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.google.common.annotations.VisibleForTesting;
import com.stripe.exception.StripeException;
import com.stripe.model.StripeObject;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Transaction;
import com.stripe.param.issuing.AuthorizationApproveParams;
import com.stripe.param.issuing.AuthorizationApproveParams.Builder;
import com.stripe.param.issuing.AuthorizationDeclineParams;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripeDirectHandler {
  private final NetworkMessageService networkMessageService;

  NetworkCommon processAuthorization(ParseRecord parseRecord, boolean isTest)
      throws StripeException {
    StripeEventType stripeEventType = parseRecord.stripeEventType();
    StripeObject stripeObject = parseRecord.stripeObject();

    NetworkCommon common = null;
    switch (stripeEventType) {
      case ISSUING_AUTHORIZATION_REQUEST -> {
        Authorization auth = (Authorization) stripeObject;
        if (auth.getStatus() != "pending") {
          // TODO(kuchlein): handle "closed" and "reversed" cases
        }
        common =
            new NetworkCommon(
                NetworkMessageType.AUTH_REQUEST, auth, parseRecord.stripeWebhookLog());
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
    }

    if (common != null && common.getNetworkMessage() != null) {
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

  void processCard(StripeEventType stripeEventType, StripeObject stripeObject) {}

  void processCardHolder(StripeEventType stripeEventType, StripeObject stripeObject) {}

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
