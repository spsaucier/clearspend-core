package com.clearspend.capital.service.type;

import com.clearspend.capital.client.i2c.push.controller.type.EventNotificationAdvanceRequest;
import com.clearspend.capital.client.i2c.push.controller.type.I2cCard;
import com.clearspend.capital.client.i2c.push.controller.type.I2cTransaction;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.CreditOrDebit;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.NetworkMessageDeviceType;
import com.clearspend.capital.data.model.enums.NetworkMessageType;
import com.stripe.model.issuing.Authorization;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class NetworkCommon {

  // the card number
  @NonNull private String cardNumber;

  @NonNull private NetworkMessageType networkMessageType;

  @NonNull private CreditOrDebit creditOrDebit;

  // will always be positive
  @NonNull private Amount requestedAmount;

  @NonNull private Amount transactionAmount;

  @NonNull private String acquirerNumber;

  @NonNull private String merchantNumber;

  @NonNull private String merchantName;

  @NonNull private ClearAddress merchantAddress;

  @NonNull private Integer merchantCategoryCode;

  @NonNull private NetworkMessageDeviceType networkMessageDeviceType;

  @NonNull private OffsetDateTime transactionDate;

  @NonNull private String i2cTransactionRef;

  @NonNull private EventNotificationAdvanceRequest request;

  private TypedId<BusinessId> businessId;

  private Allocation allocation;

  private Card card;

  private Account account;

  private boolean postHold = false;

  private boolean postAdjustment = false;

  private boolean postDecline = false;

  private AccountActivity accountActivity = new AccountActivity();

  public NetworkCommon(I2cTransaction i2cTransaction, I2cCard i2cCard) {
    cardNumber = i2cCard.getCardNumber();
    i2cCard.setEncryptedCardNumber(new NullableEncryptedString(i2cCard.getCardNumber()));
    i2cCard.setCardNumber("");
    networkMessageType = NetworkMessageType.fromMti(i2cTransaction.getMessageType());
    BigDecimal amount = new BigDecimal(i2cTransaction.getRequestedAmount());
    requestedAmount =
        Amount.of(Currency.of(i2cTransaction.getRequestedAmountCurrency()), amount.abs());
    creditOrDebit = requestedAmount.isPositive() ? CreditOrDebit.CREDIT : CreditOrDebit.DEBIT;
    transactionAmount =
        Amount.of(
            Currency.of(i2cTransaction.getTransactionCurrency()),
            new BigDecimal(i2cTransaction.getTransactionAmount()));

    acquirerNumber = i2cTransaction.getCardAcceptor().getAcquirerRef();
    merchantNumber = i2cTransaction.getCardAcceptor().getMerchantCode();
    merchantName = i2cTransaction.getCardAcceptor().getMerchantName();
    merchantAddress =
        new ClearAddress(
            "",
            "",
            i2cTransaction.getCardAcceptor().getMerchantLocality(),
            i2cTransaction.getCardAcceptor().getMerchantRegion(),
            i2cTransaction.getCardAcceptor().getMerchantPostalCode(),
            i2cTransaction.getCardAcceptor().getMerchantCountry());
    merchantCategoryCode = i2cTransaction.getCardAcceptor().getMcc();

    networkMessageDeviceType =
        NetworkMessageDeviceType.fromI2cDeviceType(
            i2cTransaction.getCardAcceptor().getDeviceType());
    transactionDate = i2cTransaction.getDate().atTime(i2cTransaction.getTime());
    i2cTransactionRef = i2cTransaction.getTransactionRef();
    request = new EventNotificationAdvanceRequest(null, i2cTransaction, i2cCard);
  }

  public NetworkCommon(Authorization auth) {}
}
