package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.StripeProperties;
import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.client.stripe.types.FinancialAccountAbaAddress;
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
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.PendingStripeTransferService;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stripe.model.Account;
import com.stripe.model.StripeObject;
import com.stripe.model.StripeRawJsonObject;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
  private final PendingStripeTransferService pendingStripeTransferService;
  private final StripeClient stripeClient;
  private final boolean standardHold;
  private final StripeProperties stripeProperties;

  public StripeConnectHandler(
      BusinessService businessService,
      BusinessBankAccountService businessBankAccountService,
      PendingStripeTransferService pendingStripeTransferService,
      StripeClient stripeClient,
      StripeProperties stripeProperties,
      @Value("${clearspend.ach.hold.standard:true}") boolean standardHold) {

    this.businessService = businessService;
    this.businessBankAccountService = businessBankAccountService;
    this.pendingStripeTransferService = pendingStripeTransferService;
    this.stripeClient = stripeClient;
    this.stripeProperties = stripeProperties;
    this.standardHold = standardHold;
  }

  public void accountUpdated(Account account) {
    try {
      Business business = businessService.retrieveBusinessByStripeAccountReference(account.getId());
      businessService.updateBusinessAccordingToStripeAccountRequirements(business, account);
    } catch (RecordNotFoundException recordNotFoundException) {
      log.info("Ignored case for record not found exception on Stripe connect webhook event.");
    }
  }

  public void externalAccountCreated(StripeObject stripeObject) {}

  public void inboundTransferCreated(StripeObject stripeObject) {}

  public void inboundTransferSucceeded(StripeObject stripeObject) {
    // TODO: Doublecheck with Stripe if this callback means that we can safely use the money or not
    // can be used as an alternative for clearspend.ach.hold.standard in test env since we receive
    // it almost immediately after issuing a transfer
    if (!standardHold) {
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
                  // We should only process this event for customer connected accounts to push
                  // money further to the Clearspend financial account
                  if (StringUtils.equals(
                      stripeProperties.getClearspendFinancialAccountId(),
                      event.getEvent().getFinancialAccount())) {
                    return;
                  }

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
                        business.getStripeData().getAccountRef(),
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

  public void financialAccountFeaturesUpdated(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, FinancialAccountEvent.class)
        .ifPresent(
            event -> financialAccountFeaturesUpdated(event.getBusinessId(), event.getEvent()));
  }

  public void financialAccountFeaturesUpdated(
      TypedId<BusinessId> businessId, FinancialAccount financialAccount) {
    if (financialAccount.getPendingFeatures().isEmpty()
        && financialAccount.getRestrictedFeatures().isEmpty()) {
      Business business =
          businessService.retrieveBusinessByStripeFinancialAccount(financialAccount.getId());
      FinancialAccountAbaAddress financialAccountAddress =
          stripeClient
              .getFinancialAccount(
                  businessId,
                  business.getStripeData().getAccountRef(),
                  business.getStripeData().getFinancialAccountRef())
              .getFinancialAddresses()
              .stream()
              .filter(a -> a.getAbaAddress() != null)
              .findFirst()
              .orElseThrow(
                  () ->
                      new RuntimeException("Stripe returned 0 aba addresses for an active account"))
              .getAbaAddress();

      businessService.updateBusinessStripeData(
          businessId,
          null,
          null,
          FinancialAccountState.READY,
          financialAccountAddress.getAccountNumber(),
          financialAccountAddress.getRoutingNumber());

      pendingStripeTransferService.executePendingStripeTransfers(businessId);

      businessService.notifyFinancialAccountReady(business);
    }
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
          businessService.retrieveBusiness(businessId, true);
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

  public static class FinancialAccountEvent extends StripeWebhookEventWrapper<FinancialAccount> {

    @Override
    protected Map<String, String> getMetadata() {
      return getEvent().getMetadata();
    }
  }
}
