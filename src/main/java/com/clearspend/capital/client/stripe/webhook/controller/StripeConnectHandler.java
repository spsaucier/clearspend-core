package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.ReceivedCredit;
import com.clearspend.capital.client.stripe.types.StripeWebhookEventWrapper;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stripe.model.Account;
import com.stripe.model.StripeObject;
import com.stripe.model.StripeRawJsonObject;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StripeConnectHandler {

  private static final Gson gson =
      new GsonBuilder()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();

  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;
  private final StripeClient stripeClient;
  private final boolean placeHold;

  public StripeConnectHandler(
      BusinessService businessService,
      BusinessBankAccountService businessBankAccountService,
      StripeClient stripeClient,
      @Value("${clearspend.ach.hold.place:true}") boolean placeHold) {

    this.businessService = businessService;
    this.businessBankAccountService = businessBankAccountService;
    this.stripeClient = stripeClient;
    this.placeHold = placeHold;
  }

  @SneakyThrows
  public void accountUpdated(StripeObject stripeObject) {
    Account account = new ObjectMapper().readValue(stripeObject.toJson(), Account.class);
    businessService.updateBusinessAccordingToStripeAccountRequirements(
        businessService.retrieveBusinessByStripeAccountReference(account.getId()), account);
  }

  public void externalAccountCreated(StripeObject stripeObject) {}

  public void inboundTransferCreated(StripeObject stripeObject) {}

  public void inboundTransferSucceeded(StripeObject stripeObject) {
    // TODO: Doublecheck with Stripe if this callback means that we can safely use the money or not
    // can be used as an alternative for clearspend.ach.hold.place in test env since we receive it
    // almost immediately after issuing a transfer
    if (!placeHold) {
      processInboundTransferResult(stripeObject, true);
    }
  }

  public void inboundTransferFailed(StripeObject stripeObject) {
    // TODO: General note for failures - revisit if should have some sort of user notification
    // (email/push/sms) in case of money movement failure
    processInboundTransferResult(stripeObject, false);
  }

  private void processInboundTransferResult(StripeObject stripeObject, boolean succeed) {
    parseBetaApiEvent(stripeObject, InboundTransferEvent.class)
        .ifPresent(
            inboundTransferEvent -> {
              Map<String, String> metadata = inboundTransferEvent.getEvent().getMetadata();

              Amount amount =
                  Amount.fromStripeAmount(
                      Currency.of(inboundTransferEvent.getEvent().getCurrency()),
                      inboundTransferEvent.getEvent().getAmount().longValue());

              businessBankAccountService.processBankAccountDepositOutcome(
                  StripeMetadataEntry.extractId(StripeMetadataEntry.BUSINESS_ID, metadata),
                  StripeMetadataEntry.extractId(StripeMetadataEntry.ADJUSTMENT_ID, metadata),
                  StripeMetadataEntry.extractId(StripeMetadataEntry.HOLD_ID, metadata),
                  amount,
                  succeed);
            });
  }

  public void receivedCreditCreated(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, ReceivedCreditEvent.class)
        .ifPresent(
            event -> {
              ReceivedCredit receivedCredit = event.getEvent();
              switch (receivedCredit.getNetwork()) {
                case "stripe" -> {
                  try {
                    Business business =
                        businessService.retrieveBusinessByStripeFinancialAccount(
                            receivedCredit.getFinancialAccount());
                    BusinessBankAccount businessBankAccount =
                        businessBankAccountService
                            .getBusinessBankAccounts(business.getId(), true)
                            .get(0);

                    stripeClient.executeOutboundTransfer(
                        business.getId(),
                        business.getStripeAccountReference(),
                        receivedCredit.getFinancialAccount(),
                        businessBankAccount.getStripeBankAccountRef(),
                        Amount.fromStripeAmount(
                            Currency.of(receivedCredit.getCurrency()),
                            receivedCredit.getAmount().longValue()),
                        "ACH push",
                        "ACH push");
                  } catch (RecordNotFoundException e) {
                    log.info(
                        "Skipping received credit event for business since it is not found in the db by stripe financial account {}",
                        receivedCredit.getFinancialAccount());
                  } catch (Exception e) {
                    log.error("Failed to create outbound payment", e);
                  }
                }
                case "ach" -> {
                  // TODO: Call StripeClient.pushFundsToClearspendFinancialAccount to move
                  // funds to the platform account. But so far we do not receive it for ach
                  // transfers
                }
                default -> log.error(
                    "Unexpected network value {} for inbound transfer",
                    receivedCredit.getNetwork());
              }
            });
  }

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

  private <T extends StripeWebhookEventWrapper<?>> Optional<T> parseBetaApiEvent(
      StripeObject stripeObject, Class<T> clazz) {
    T event = null;
    if (stripeObject instanceof StripeRawJsonObject) {
      event = gson.fromJson(stripeObject.toJson(), clazz);

      // Since callback can receive events from multiple environments filter them by business id
      // but some events do not have metadata like received_credit since they are initiated by
      // Stripe
      TypedId<BusinessId> businessId = event.getBusinessId();
      if (businessId != null) {
        try {
          businessService.retrieveBusiness(businessId);
        } catch (RecordNotFoundException e) {
          log.info("Skipping event for business id {} since it is not found in the db", businessId);
          return Optional.empty();
        }
      }
    } else {
      log.error(
          "Failed to parse beta api event. Actual class is {}. Most probably it is already supported by stripe sdk",
          stripeObject.getClass().getName());
    }

    return Optional.ofNullable(event);
  }

  public static class InboundTransferEvent extends StripeWebhookEventWrapper<InboundTransfer> {

    @Override
    protected Map<String, String> getMetadata() {
      return getEvent().getMetadata();
    }
  }

  public static class ReceivedCreditEvent extends StripeWebhookEventWrapper<ReceivedCredit> {

    @Override
    protected Map<String, String> getMetadata() {
      return Map.of();
    }
  }
}
