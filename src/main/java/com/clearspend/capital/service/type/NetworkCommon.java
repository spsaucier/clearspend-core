package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
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

@Data
@RequiredArgsConstructor
public class NetworkCommon {

  // the identifier for this card at Stripe
  @NonNull private String cardExternalRef;

  @NonNull private NetworkMessageType networkMessageType;

  boolean allowPartialApproval;

  // will always be positive
  @NonNull private Amount requestedAmount;

  @NonNull private String merchantNumber;

  @NonNull private String merchantName;

  @NonNull private ClearAddress merchantAddress;

  @NonNull private Integer merchantCategoryCode;

  @NonNull private MerchantType merchantType;

  @NonNull private OffsetDateTime transactionDate;

  // Stripe's identifier for this networkMessage
  @NonNull private String externalRef;

  @NonNull private String request;

  // the amount we are approving this transaction for. Will be less than or equal to requestedAmount
  @NonNull private Amount approvedAmount;

  private String stripeAuthorizationExternalRef;

  private TypedId<BusinessId> businessId;

  private Allocation allocation;

  private Card card;

  private Account account;

  private NetworkMessage networkMessage;
  private UUID networkMessageGroupId = UUID.randomUUID();

  private AccountActivity accountActivity;

  private boolean postHold = false;
  private Hold hold;
  // list of holds that we need to update (typically to change the status)
  private List<Hold> updatedHolds = new ArrayList<>();

  private boolean postAdjustment = false;
  private Adjustment adjustment;

  private boolean postDecline = false;

  private List<DeclineReason> declineReasons = new ArrayList<>();

  private AccountActivityDetails accountActivityDetails = new AccountActivityDetails();

  // Stripe authorizations
  public NetworkCommon(
      NetworkMessageType networkMessageType, Authorization authorization, String rawJson) {
    cardExternalRef = authorization.getCard().getId();
    Currency currency = Currency.of(authorization.getCurrency());
    Amount amount = Amount.fromStripeAmount(currency, authorization.getAmount());
    if (authorization.getPendingRequest() != null) {
      allowPartialApproval = authorization.getPendingRequest().getIsAmountControllable();
      amount = Amount.fromStripeAmount(currency, authorization.getPendingRequest().getAmount());
    }
    // amounts from Stripe for authorization requests are always debits and positive
    amount = amount.negate();
    amount.ensureNegative();
    this.networkMessageType = networkMessageType;
    // we do all our processing with positive amounts and rely on cr
    requestedAmount = amount;
    approvedAmount = Amount.of(amount.getCurrency());

    if (authorization.getMerchantData() != null) {
      MerchantData merchantData = authorization.getMerchantData();
      merchantNumber = merchantData.getNetworkId();
      merchantName = merchantData.getName();
      merchantAddress = getMerchantAddress(merchantData);
      merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());
      merchantType = MerchantType.fromString(merchantData.getCategory());
    }

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(authorization.getCreated()), ZoneOffset.UTC);
    externalRef = authorization.getId();

    request = rawJson;
  }

  // Stripe completions (or more incorrect captures)
  public NetworkCommon(Transaction transaction, String rawJson) {
    cardExternalRef = transaction.getCard();
    networkMessageType = NetworkMessageType.TRANSACTION_CREATED;
    Currency currency = Currency.of(transaction.getCurrency());
    Amount amount = Amount.fromStripeAmount(currency, transaction.getAmount());
    requestedAmount = amount;
    approvedAmount = Amount.of(amount.getCurrency());

    if (transaction.getMerchantData() != null) {
      MerchantData merchantData = transaction.getMerchantData();
      merchantNumber = merchantData.getNetworkId();
      merchantName = merchantData.getName();
      merchantAddress = getMerchantAddress(merchantData);
      merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());
      merchantType = MerchantType.fromString(merchantData.getCategory());
    }

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(transaction.getCreated()), ZoneOffset.UTC);
    externalRef = transaction.getId();
    stripeAuthorizationExternalRef = transaction.getAuthorization();

    request = rawJson;
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
}
