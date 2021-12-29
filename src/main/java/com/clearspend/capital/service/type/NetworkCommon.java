package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.network.CreditOrDebit;
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
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class NetworkCommon {

  // the identifier for this card at Stripe
  @NonNull private String cardRef;

  @NonNull private NetworkMessageType networkMessageType;

  boolean allowPartialApproval;

  @NonNull private CreditOrDebit creditOrDebit;

  // will always be positive
  @NonNull private Amount requestedAmount;

  @NonNull private String merchantNumber;

  @NonNull private String merchantName;

  @NonNull private ClearAddress merchantAddress;

  @NonNull private Integer merchantCategoryCode;

  @NonNull private OffsetDateTime transactionDate;

  @NonNull private String externalRef;

  @NonNull private String request;

  // the amount we are approving this transaction for. Will be less than or equal to requestedAmount
  @NonNull private Amount approvedAmount;

  private TypedId<BusinessId> businessId;

  private Allocation allocation;

  private Card card;

  private Account account;

  private boolean postHold = false;

  private boolean postAdjustment = false;

  private boolean postDecline = false;

  private List<DeclineReason> declineReasons = new ArrayList<>();

  private AccountActivity accountActivity = new AccountActivity();

  // Stripe authorizations
  public NetworkCommon(Authorization authorization, String rawJson) {
    cardRef = authorization.getCard().getId();
    networkMessageType = NetworkMessageType.PRE_AUTH;
    Currency currency = Currency.of(authorization.getCurrency());
    Amount amount = Amount.fromStripeAmount(currency, authorization.getAmount());
    if (authorization.getPendingRequest() != null) {
      allowPartialApproval = authorization.getPendingRequest().getIsAmountControllable();
      amount = Amount.fromStripeAmount(currency, authorization.getPendingRequest().getAmount());
    }
    amount.ensureNonNegative();
    creditOrDebit = CreditOrDebit.DEBIT; // assuming all authorizations are debits
    requestedAmount = amount;
    approvedAmount = Amount.of(amount.getCurrency());

    MerchantData merchantData = authorization.getMerchantData();
    merchantNumber = merchantData.getNetworkId();
    merchantName = merchantData.getName();
    merchantAddress =
        new ClearAddress(
            "",
            "",
            merchantData.getCity(),
            merchantData.getState(),
            merchantData.getPostalCode(),
            Country.of(merchantData.getCountry()));
    merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(authorization.getCreated()), ZoneOffset.UTC);
    externalRef = authorization.getId();

    request = rawJson;
  }

  // Stripe completions (or more incorrect captures)
  public NetworkCommon(Transaction transaction, String rawJson) {
    cardRef = transaction.getCard();
    networkMessageType = NetworkMessageType.FINANCIAL_AUTH;
    Currency currency = Currency.of(transaction.getCurrency());
    Amount amount = Amount.fromStripeAmount(currency, transaction.getAmount());
    creditOrDebit = amount.isPositive() ? CreditOrDebit.CREDIT : CreditOrDebit.DEBIT;
    requestedAmount = amount;
    approvedAmount = Amount.of(amount.getCurrency());

    MerchantData merchantData = transaction.getMerchantData();
    merchantNumber = merchantData.getNetworkId();
    merchantName = merchantData.getName();
    merchantAddress =
        new ClearAddress(
            "",
            "",
            merchantData.getCity(),
            merchantData.getState(),
            merchantData.getPostalCode(),
            Country.of(merchantData.getCountry()));
    merchantCategoryCode = Integer.parseInt(merchantData.getCategoryCode());

    transactionDate =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(transaction.getCreated()), ZoneOffset.UTC);
    externalRef = transaction.getId();

    request = rawJson;
  }
}
