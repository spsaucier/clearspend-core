package com.clearspend.capital.service.type;

import com.clearspend.capital.client.stripe.webhook.controller.StripeEventType;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Decline;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.network.NetworkMessage;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.MerchantData;
import com.stripe.model.issuing.Transaction;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

  // an approximate address for the merchant. Note that it's not a full address given how the data
  // comes to us
  @NonNull private ClearAddress merchantAddress;

  // a.k.a. MCC (4 digits)
  @NonNull private Integer merchantCategoryCode;

  // the type of merchant which is defined by Stripe
  @NonNull private MerchantType merchantType;

  @NonNull private OffsetDateTime transactionDate;

  // Stripe's identifier for this networkMessage
  @NonNull private String externalRef;

  // the raw request from Stripe
  @NonNull private String request;

  // the amount we are approving this transaction for. Will be less than or equal to requestedAmount
  @NonNull private Amount approvedAmount;

  private String stripeAuthorizationExternalRef;

  private TypedId<BusinessId> businessId;

  private Allocation allocation;

  private Card card;

  private Account account;

  private NetworkMessage networkMessage;
  public NetworkMessage earliestNetworkMessage;
  private List<NetworkMessage> priorNetworkMessages;
  private UUID networkMessageGroupId = UUID.randomUUID();

  private AccountActivity accountActivity;

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

  private List<DeclineReason> declineReasons = new ArrayList<>();

  private AccountActivityDetails accountActivityDetails = new AccountActivityDetails();

  // The type of Stripe event being processed. This isn't used in the actual processing (we use
  // networkMessageType for that) but is used in tests
  StripeEventType stripeEventType;
  StripeWebhookLog stripeWebhookLog;

  // Stripe authorizations
  public NetworkCommon(NetworkMessageType type, Authorization authorization, StripeWebhookLog log) {
    cardExternalRef = authorization.getCard().getId();
    Currency currency = Currency.of(authorization.getCurrency());
    Amount amount = Amount.fromStripeAmount(currency, authorization.getAmount());
    if (authorization.getPendingRequest() != null) {
      allowPartialApproval = authorization.getPendingRequest().getIsAmountControllable();
      amount = Amount.fromStripeAmount(currency, authorization.getPendingRequest().getAmount());
    }
    // amounts from Stripe for authorization requests are always debits and positive
    amount = amount.negate().ensureNegative();
    networkMessageType = type;
    isIncrementalAuthorization = authorization.getApproved();
    if (isIncrementalAuthorization) {
      priorHoldAmount = Amount.fromStripeAmount(currency, authorization.getAmount());
    }
    // we do all our processing with positive amounts and rely on cr
    requestedAmount = amount;
    paddedAmount = requestedAmount;
    approvedAmount = Amount.of(amount.getCurrency());

    if (authorization.getMerchantData() != null) {
      MerchantData merchantData = authorization.getMerchantData();
      merchantNumber = merchantData.getNetworkId();
      merchantName = merchantData.getName();
      merchantAddress = getMerchantAddress(merchantData);
      merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());
      merchantType = MerchantType.fromString(merchantData.getCategory().toLowerCase());
    }

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(authorization.getCreated()), ZoneOffset.UTC);
    externalRef = authorization.getId();
    stripeAuthorizationExternalRef = authorization.getId();

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

    if (transaction.getMerchantData() != null) {
      MerchantData merchantData = transaction.getMerchantData();
      merchantNumber = merchantData.getNetworkId();
      merchantName = merchantData.getName();
      merchantAddress = getMerchantAddress(merchantData);
      merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());
      merchantType = MerchantType.fromString(merchantData.getCategory().toLowerCase());
    }

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(transaction.getCreated()), ZoneOffset.UTC);
    externalRef = transaction.getId();
    stripeAuthorizationExternalRef = transaction.getAuthorization();

    stripeWebhookLog = log;
  }

  private ClearAddress getMerchantAddress(MerchantData merchantData) {
    return new ClearAddress(
        "",
        "",
        merchantData.getCity(),
        merchantData.getState(),
        merchantData.getPostalCode(),
        Country.of(merchantData.getCountry()));
  }

  public NetworkMessage toNetworkMessage() {
    NetworkMessage networkMessage =
        new NetworkMessage(
            businessId,
            allocation.getId(),
            account.getId(),
            networkMessageGroupId,
            networkMessageType,
            requestedAmount,
            approvedAmount,
            merchantName,
            merchantAddress,
            merchantNumber,
            merchantCategoryCode,
            externalRef);

    networkMessage.setSubType(networkMessageSubType);

    return networkMessage;
  }
}
