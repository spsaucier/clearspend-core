package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.StripeProperties;
import com.clearspend.capital.client.stripe.types.Account;
import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.client.stripe.types.FinancialAccountAbaAddress;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.OutboundTransfer;
import com.clearspend.capital.client.stripe.types.ReceivedCredit;
import com.clearspend.capital.client.stripe.types.StripeWebhookEventWrapper;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InvalidKycStepException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessBankAccountService.StripeBankAccountOp;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.PendingStripeTransferService;
import com.clearspend.capital.service.TwilioService;
import com.clearspend.capital.service.kyc.BusinessKycStepHandler;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.StripeRawJsonObject;
import java.util.ArrayList;
import java.util.List;
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
  private final TwilioService twilioService;
  private final StripeClient stripeClient;
  private final long achReturnFee;
  private final boolean standardHold;
  private final StripeProperties stripeProperties;
  private final BusinessKycStepHandler stepHandler;

  public StripeConnectHandler(
      BusinessService businessService,
      BusinessBankAccountService businessBankAccountService,
      PendingStripeTransferService pendingStripeTransferService,
      TwilioService twilioService,
      StripeClient stripeClient,
      StripeProperties stripeProperties,
      @Value("${clearspend.ach.hold.standard:true}") boolean standardHold,
      @Value("${clearspend.ach.return.fee:0}") long achReturnFee,
      BusinessKycStepHandler stepHandler) {

    this.businessService = businessService;
    this.businessBankAccountService = businessBankAccountService;
    this.pendingStripeTransferService = pendingStripeTransferService;
    this.twilioService = twilioService;
    this.stripeClient = stripeClient;
    this.stripeProperties = stripeProperties;
    this.achReturnFee = achReturnFee;
    this.standardHold = standardHold;
    this.stepHandler = stepHandler;
  }

  public void accountUpdated(StripeObject stripeObject) {
    try {
      Account account =
          gson.fromJson(
              ((Event) stripeObject).getDataObjectDeserializer().getRawJson(), Account.class);
      Business business = businessService.retrieveBusinessByStripeAccountReference(account.getId());
      stepHandler.execute(business, account);
    } catch (RecordNotFoundException recordNotFoundException) {
      log.info("Ignored case for record not found exception on Stripe connect webhook event.");
    } catch (JsonSyntaxException | InvalidKycStepException jsonSyntaxException) {
      log.error(jsonSyntaxException.getMessage());
    }
  }

  public void externalAccountCreated(StripeObject stripeObject) {}

  public void inboundTransferCreated(StripeObject stripeObject) {}

  public void inboundTransferSucceeded(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, InboundTransferEvent.class)
        .ifPresent(
            inboundTransferEvent -> processInboundTransferResult(inboundTransferEvent.getEvent()));
  }

  public void inboundTransferFailed(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, InboundTransferEvent.class)
        .ifPresent(
            inboundTransferEvent -> processInboundTransferResult(inboundTransferEvent.getEvent()));
  }

  @VisibleForTesting
  @StripeBankAccountOp(
      reviewer = "Craig Miller",
      explanation = "This is a Stripe operation that needs to work with bank accounts")
  void processInboundTransferResult(InboundTransfer inboundTransfer) {
    Map<String, String> metadata = inboundTransfer.getMetadata();

    Amount amount =
        Amount.fromStripeAmount(
            Currency.of(inboundTransfer.getCurrency()), inboundTransfer.getAmount());

    List<DeclineReason> declineReasons = new ArrayList<>();
    if (inboundTransfer.getFailureDetails() != null) {
      // TODO: General note for failures - revisit if should have some sort of user
      // notification (email/push/sms) in case of money movement failure
      declineReasons.add(
          DeclineReason.fromStripeTransferFailure(inboundTransfer.getFailureDetails().getCode()));
    }

    businessBankAccountService.processBankAccountDepositOutcome(
        StripeMetadataEntry.extractId(StripeMetadataEntry.BUSINESS_ID, metadata),
        StripeMetadataEntry.extractId(StripeMetadataEntry.ADJUSTMENT_ID, metadata),
        StripeMetadataEntry.extractId(StripeMetadataEntry.HOLD_ID, metadata),
        amount,
        declineReasons);
  }

  public void receivedCreditCreated(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, ReceivedCreditEvent.class)
        .ifPresent(
            event -> {
              ReceivedCredit receivedCredit = event.getEvent();
              switch (receivedCredit.getNetwork()) {
                case "stripe" -> onStripeCreditsReceived(receivedCredit);
                case "ach", "us_domestic_wire" -> onAchCreditsReceived(receivedCredit);
                default -> log.error(
                    "Unexpected network value {} for credit received event",
                    receivedCredit.getNetwork());
              }
            });
  }

  @VisibleForTesting
  void processOutboundTransferResult(OutboundTransfer outboundTransfer) {
    TypedId<BusinessId> businessId =
        StripeMetadataEntry.extractId(
            StripeMetadataEntry.BUSINESS_ID, outboundTransfer.getMetadata());

    Amount amount =
        Amount.fromStripeAmount(
            Currency.of(outboundTransfer.getCurrency()), outboundTransfer.getAmount());

    switch (outboundTransfer.getStatus()) {
      case "posted", "processing" -> {} // do nothing since we assume the happy path by default
      case "canceled" -> businessBankAccountService.processBankAccountWithdrawFailure(
          businessId, amount, List.of(DeclineReason.ST_CANCELLED));
      case "failed" -> businessBankAccountService.processBankAccountWithdrawFailure(
          businessId, amount, List.of(DeclineReason.ST_FAILED));
      case "returned" -> businessBankAccountService.processBankAccountWithdrawFailure(
          businessId,
          amount,
          List.of(
              DeclineReason.fromStripeTransferFailure(
                  outboundTransfer.getReturnedDetails().getCode())));
      default -> log.error(
          "Unknown outbound transfer status received: " + outboundTransfer.getStatus());
    }
  }

  /**
   * Stripe credit received might be a result of 2 money movement operations: Clearspend FA ->
   * Customer FA (in this case we need to push money further to the customer external bank account)
   * Customer FA -> Clearspend FA (no need to do anything since it is the final step of the incoming
   * transfer)
   *
   * @param receivedCredit Received credit event object from Stripe
   */
  private void onStripeCreditsReceived(ReceivedCredit receivedCredit) {
    if (StringUtils.equals(
        stripeProperties.getClearspendFinancialAccountId(), receivedCredit.getFinancialAccount())) {
      return;
    }

    try {
      Business business =
          businessService.retrieveBusinessByStripeFinancialAccount(
              receivedCredit.getFinancialAccount());
      BusinessBankAccount businessBankAccount =
          businessBankAccountService.getBusinessBankAccounts(business.getId(), true).get(0);

      stripeClient.executeOutboundTransfer(
          business.getId(),
          business.getStripeData().getAccountRef(),
          receivedCredit.getFinancialAccount(),
          businessBankAccount.getStripeBankAccountRef(),
          Amount.fromStripeAmount(
              Currency.of(receivedCredit.getCurrency()), receivedCredit.getAmount().longValue()),
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

  @VisibleForTesting
  @StripeBankAccountOp(
      reviewer = "Craig Miller",
      explanation = "This is a Stripe operation that needs to work with bank accounts")
  void onAchCreditsReceived(ReceivedCredit receivedCredit) {
    try {
      Business business =
          businessService.retrieveBusinessByStripeFinancialAccount(
              receivedCredit.getFinancialAccount());

      businessBankAccountService.processExternalAchTransfer(
          business.getId(),
          Amount.fromStripeAmount(
              Currency.of(receivedCredit.getCurrency()), receivedCredit.getAmount().longValue()),
          false);
    } catch (RecordNotFoundException e) {
      log.info(
          "Skipping received credit event for an ach transfer for business since it is not found in the db by stripe financial account {}",
          receivedCredit.getFinancialAccount());
    } catch (Exception e) {
      log.error("Failed to process credit event for an ach transfer", e);
    }
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
      // although we have financial addresses in the event we still need to get them from stripe
      // since account number is truncated in the event object
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
                      new RuntimeException(
                          "Stripe returned 0 aba addresses for an active financial account: "
                              + financialAccount.getId()))
              .getAbaAddress();

      businessService.updateBusinessStripeData(
          businessId,
          null,
          null,
          FinancialAccountState.READY,
          financialAccountAddress.getAccountNumber(),
          financialAccountAddress.getRoutingNumber());

      pendingStripeTransferService.executePendingStripeTransfers(businessId);

      // send notification only to onboarded customers who haven't selected plaid route
      if (business.getStatus() == BusinessStatus.ACTIVE
          && businessBankAccountService.getBusinessBankAccounts(businessId, true).isEmpty()) {
        twilioService.sendFinancialAccountReadyEmail(
            business.getBusinessEmail().getEncrypted(), business.getLegalName());
      }
    }
  }

  public void outboundTransferCreated(StripeObject stripeObject) {}

  public void outboundTransferReturned(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, OutboundTransferEvent.class)
        .ifPresent(
            outboundTransferEvent ->
                processOutboundTransferResult(outboundTransferEvent.getEvent()));
  }

  public void outboundTransferFailed(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, OutboundTransferEvent.class)
        .ifPresent(
            outboundTransferEvent ->
                processOutboundTransferResult(outboundTransferEvent.getEvent()));
  }

  public void outboundTransferCancelled(StripeObject stripeObject) {
    parseBetaApiEvent(stripeObject, OutboundTransferEvent.class)
        .ifPresent(
            outboundTransferEvent ->
                processOutboundTransferResult(outboundTransferEvent.getEvent()));
  }

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

  public static class OutboundTransferEvent extends StripeWebhookEventWrapper<OutboundTransfer> {

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
