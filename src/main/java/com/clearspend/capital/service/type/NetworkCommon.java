package com.clearspend.capital.service.type;

import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.types.TransactionType;
import com.clearspend.capital.client.stripe.webhook.controller.StripeEventType;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.decline.Decline;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.enums.network.VerificationResultType;
import com.clearspend.capital.data.model.network.NetworkMessage;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.MerchantData;
import com.stripe.model.issuing.Authorization.PendingRequest;
import com.stripe.model.issuing.Authorization.VerificationData;
import com.stripe.model.issuing.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This type is used for all payments processing operations from Stripe. This is a canonical data
 * type that's create based on teh request and then decorated by us during processing. Generally the
 * fields that are marked as @NonNull are from Stripe, the ones that aren't are populated by us
 * during processing.
 *
 * <p>There are a number of fields that hold data that isn't strictly needed but comes in handy when
 * writing tests (e.g. hold, adjustment, decline, stripeWebhookLog records). If this causes any type
 * of performance issues they can be removed.
 *
 * <p>Payment operations from Stripe come in the form of either an Authorization or Transaction.
 * There is a constructor for both types.
 */
@Data
@RequiredArgsConstructor
public class NetworkCommon {
  private static final Configuration jsonPathConfiguration =
      Configuration.defaultConfiguration()
          .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS);

  private static final Map<NetworkMessageType, AccountActivityType> accountActivityMappings =
      Map.of(
          NetworkMessageType.AUTH_REQUEST, AccountActivityType.NETWORK_AUTHORIZATION,
          NetworkMessageType.AUTH_CREATED, AccountActivityType.NETWORK_AUTHORIZATION,
          NetworkMessageType.AUTH_UPDATED, AccountActivityType.NETWORK_AUTHORIZATION,
          NetworkMessageType.TRANSACTION_CREATED, AccountActivityType.NETWORK_CAPTURE);

  // the identifier for this card at Stripe
  @NonNull private String cardExternalRef;

  // the type of network message we're processing from Stripe
  @NonNull private NetworkMessageType networkMessageType;

  private String networkMessageSubType;

  // flag to indicate we can give a lower approval than asked for
  boolean allowPartialApproval = false;

  // flag to indicate we are increasing the value of an existing authorization
  boolean isIncrementalAuthorization = false;
  // the amount held by Stripe prior to this request or zero if none
  @NonNull private Amount priorHoldAmount = Amount.of(Currency.UNSPECIFIED);

  // the amount that the merchant is asking for
  @NonNull private Amount requestedAmount;

  // the amount that the merchant asked for including padding
  @NonNull private Amount paddedAmount;

  // a.k.a. MID or merchant ID
  @NonNull private String merchantNumber;

  // the name of the merchant from the location field in the ISO message
  @NonNull private String merchantName;

  // the amount that the merchant is asking for in the merchant currency
  @NonNull private Amount merchantAmount;

  // an approximate address for the merchant. Note that it's not a full address given how the data
  // comes to us
  @NonNull private ClearAddress merchantAddress;

  // a.k.a. MCC (4 digits)
  @NonNull private Integer merchantCategoryCode;

  // the type of merchant which is defined by Stripe
  @NonNull private MerchantType merchantType;

  private String merchantStatementDescriptor;
  private String enhancedMerchantLogo;

  @NonNull private OffsetDateTime transactionDate;

  // Stripe's identifier for this networkMessage
  @NonNull private String externalRef;

  // the raw request from Stripe
  @NonNull private String request;

  // the amount we are approving this transaction for. Will be less than or equal to requestedAmount
  @NonNull private Amount approvedAmount;

  @NonNull private VerificationResultType addressPostalCodeCheck = VerificationResultType.UNKNOWN;
  @NonNull private VerificationResultType cvcCheck = VerificationResultType.UNKNOWN;
  @NonNull private VerificationResultType expiryCheck = VerificationResultType.UNKNOWN;

  // will be filled if addressPostalCodeCheck = MISMATCH
  private String addressPostalCode;

  private AuthorizationMethod authorizationMethod;

  private Boolean foreign;

  private String stripeAuthorizationExternalRef;

  private Business business;

  private Allocation allocation;

  private Card card;

  private User user;

  private Account account;

  private NetworkMessage networkMessage;
  public NetworkMessage earliestNetworkMessage;
  private List<NetworkMessage> priorNetworkMessages;
  private UUID networkMessageGroupId = UUID.randomUUID();

  private AccountActivity accountActivity;
  // holds prior account activity, if any, so we can copy data from it to newly created records
  private AccountActivity priorAccountActivity;

  private boolean postHold = false;
  private OffsetDateTime holdExpiration;
  private Amount holdAmount;
  private Hold hold;
  private Hold priorHold;
  // list of holds that we need to update (typically to change the status)
  private List<Hold> updatedHolds = new ArrayList<>();

  private boolean postAdjustment = false;
  private AdjustmentRecord adjustmentRecord;

  private boolean postDecline = false;
  private Decline decline;

  private List<DeclineDetails> declineDetails = new ArrayList<>();

  private AccountActivityDetails accountActivityDetails = new AccountActivityDetails();

  private BigDecimal interchange;

  // The type of Stripe event being processed. This isn't used in the actual processing (we use
  // networkMessageType for that) but is used in tests
  StripeEventType stripeEventType;
  StripeWebhookLog stripeWebhookLog;

  // Stripe authorizations
  public NetworkCommon(NetworkMessageType type, Authorization authorization, StripeWebhookLog log) {
    cardExternalRef = authorization.getCard().getId();
    Currency currency = Currency.of(authorization.getCurrency());
    Amount amount = Amount.fromStripeAmount(currency, authorization.getAmount());
    PendingRequest pendingRequest = authorization.getPendingRequest();
    if (pendingRequest != null) {
      allowPartialApproval = pendingRequest.getIsAmountControllable();
      amount = Amount.fromStripeAmount(currency, pendingRequest.getAmount());
      merchantAmount =
          Amount.fromStripeAmount(
              Currency.of(pendingRequest.getMerchantCurrency()),
              pendingRequest.getMerchantAmount());
    } else {
      merchantAmount =
          Amount.fromStripeAmount(
              Currency.of(authorization.getMerchantCurrency()), authorization.getMerchantAmount());
    }
    // amounts from Stripe for authorization requests are always debits and zero or positive
    amount = amount.negate().ensureLessThanOrEqualToZero();
    networkMessageType = type;
    isIncrementalAuthorization = authorization.getApproved();
    if (isIncrementalAuthorization) {
      priorHoldAmount = Amount.fromStripeAmount(currency, authorization.getAmount());
    }
    // we do all our processing with positive amounts and rely on cr
    requestedAmount = amount;
    paddedAmount = requestedAmount;
    approvedAmount = Amount.of(amount.getCurrency());
    authorizationMethod = AuthorizationMethod.fromString(authorization.getAuthorizationMethod());

    if (authorization.getMerchantData() != null) {
      MerchantData merchantData = authorization.getMerchantData();
      merchantNumber = merchantData.getNetworkId();
      merchantName = merchantData.getName();
      merchantAddress = getMerchantAddress(merchantData);
      merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());
      merchantType = MerchantType.fromStripe(merchantData.getCategory().toLowerCase());

      foreign =
          !Objects.equals(Country.of(merchantData.getCountry()), Country.USA)
              || !Objects.equals(Currency.of(authorization.getMerchantCurrency()), Currency.USD);
    } else {
      foreign = !Objects.equals(Currency.of(authorization.getMerchantCurrency()), Currency.USD);
    }

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(authorization.getCreated()), ZoneOffset.UTC);
    externalRef = authorization.getId();
    stripeAuthorizationExternalRef = authorization.getId();

    VerificationData verificationData = authorization.getVerificationData();
    if (verificationData != null) {
      addressPostalCodeCheck =
          VerificationResultType.fromStripe(verificationData.getAddressPostalCodeCheck());
      if (addressPostalCodeCheck == VerificationResultType.MISMATCH) {
        // TODO: Should be reworked to stripe sdk once they support this field
        addressPostalCode =
            JsonPath.using(jsonPathConfiguration)
                .parse(log.getRequest())
                .read("$.data.object.verification_data.postal_code");
      }
      cvcCheck = VerificationResultType.fromStripe(verificationData.getCvcCheck());
      expiryCheck = VerificationResultType.fromStripe(verificationData.getExpiryCheck());
    }

    stripeWebhookLog = log;
  }

  // Stripe completions (or more incorrect captures)
  public NetworkCommon(Transaction transaction, StripeWebhookLog log) {
    cardExternalRef = transaction.getCard();
    networkMessageType = NetworkMessageType.TRANSACTION_CREATED;
    networkMessageSubType = transaction.getType();
    Currency currency = Currency.of(transaction.getCurrency());
    Amount amount = Amount.fromStripeAmount(currency, transaction.getAmount());
    requestedAmount = amount;
    paddedAmount = requestedAmount;
    approvedAmount = Amount.of(amount.getCurrency());

    merchantAmount =
        Amount.fromStripeAmount(
            Currency.of(transaction.getMerchantCurrency()), transaction.getMerchantAmount());

    if (transaction.getMerchantData() != null) {
      MerchantData merchantData = transaction.getMerchantData();
      merchantNumber = merchantData.getNetworkId();
      merchantName = merchantData.getName();
      merchantAddress = getMerchantAddress(merchantData);
      merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());
      merchantType = MerchantType.fromStripe(merchantData.getCategory().toLowerCase());

      foreign =
          !Objects.equals(Country.of(merchantData.getCountry()), Country.USA)
              || !Objects.equals(Currency.of(transaction.getMerchantCurrency()), Currency.USD);
    } else {
      foreign = !Objects.equals(Currency.of(transaction.getMerchantCurrency()), Currency.USD);
    }

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(transaction.getCreated()), ZoneOffset.UTC);
    externalRef = transaction.getId();
    stripeAuthorizationExternalRef = transaction.getAuthorization();

    // TODO: Should be reworked to stripe sdk once they support this field
    String rawInterchange =
        JsonPath.using(jsonPathConfiguration)
            .parse(log.getRequest())
            .read("$.data.object.interchange.amount_decimal");
    if (rawInterchange != null) {
      interchange = new BigDecimal(rawInterchange);
    }

    stripeWebhookLog = log;
  }

  private ClearAddress getMerchantAddress(MerchantData merchantData) {
    return new ClearAddress(
        "",
        "",
        merchantData.getCity() != null ? merchantData.getCity() : "",
        merchantData.getState() != null ? merchantData.getState() : "",
        merchantData.getPostalCode() != null ? merchantData.getPostalCode() : "",
        Country.of(merchantData.getCountry()));
  }

  public NetworkMessage toNetworkMessage() {
    NetworkMessage networkMessage =
        new NetworkMessage(
            business.getId(),
            networkMessageGroupId,
            networkMessageType,
            requestedAmount,
            approvedAmount,
            merchantName,
            merchantAmount,
            merchantAddress,
            merchantNumber,
            merchantCategoryCode,
            externalRef);

    networkMessage.setSubType(networkMessageSubType);
    networkMessage.setInterchange(interchange);
    Optional.ofNullable(allocation)
        .map(Allocation::getId)
        .ifPresent(networkMessage::setAllocationId);
    Optional.ofNullable(account).map(Account::getId).ifPresent(networkMessage::setAccountId);

    return networkMessage;
  }

  public Map<String, String> getMetadata() {
    Map<String, String> metadata = new HashMap<>();

    if (business != null && business.getId() != null) {
      metadata.put(StripeMetadataEntry.BUSINESS_ID.getKey(), business.getId().toString());
    }
    if (getAllocation() != null && getAllocation().getId() != null) {
      metadata.put(StripeMetadataEntry.ALLOCATION_ID.getKey(), getAllocation().getId().toString());
    }
    if (getCard() != null && getCard().getId() != null) {
      metadata.put(StripeMetadataEntry.CARD_ID.getKey(), getCard().getId().toString());
    }
    if (getAccount() != null && getAccount().getId() != null) {
      metadata.put(StripeMetadataEntry.ACCOUNT_ID.getKey(), getAllocation().getId().toString());
    }
    if (this.getDeclineDetails() != null && !this.getDeclineDetails().isEmpty()) {
      metadata.put(
          StripeMetadataEntry.DECLINE_REASONS.getKey(),
          this.getDeclineDetails().stream()
              .map(reason -> reason.getReason().toString())
              .collect(Collectors.joining(",")));
    }

    if (getNetworkMessage() != null) {
      if (getNetworkMessage().getId() != null) {
        metadata.put(
            StripeMetadataEntry.NETWORK_MESSAGE_ID.getKey(),
            getNetworkMessage().getId().toString());
      }
      if (getNetworkMessage().getAdjustmentId() != null) {
        metadata.put(
            StripeMetadataEntry.ADJUSTMENT_ID.getKey(),
            getNetworkMessage().getAdjustmentId().toString());
      }
      if (getNetworkMessage().getHoldId() != null) {
        metadata.put(
            StripeMetadataEntry.HOLD_ID.getKey(), getNetworkMessage().getHoldId().toString());
      }
    }

    return metadata;
  }

  public AccountActivityType getAccountActivityType() {
    if (networkMessageType == NetworkMessageType.TRANSACTION_CREATED
        && TransactionType.from(networkMessageSubType) == TransactionType.REFUND) {
      return AccountActivityType.NETWORK_REFUND;
    }

    return accountActivityMappings.get(networkMessageType);
  }
}
