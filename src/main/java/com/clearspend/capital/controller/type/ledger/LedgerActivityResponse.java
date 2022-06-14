package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.activity.PaymentDetails;
import com.clearspend.capital.controller.type.activity.ReceiptDetails;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

@Setter
@Getter
@RequiredArgsConstructor
public class LedgerActivityResponse {

  @JsonProperty("accountActivityId")
  @NonNull
  private TypedId<AccountActivityId> accountActivityId;

  @JsonProperty("activityTime")
  @NonNull
  private OffsetDateTime activityTime;

  @JsonProperty("type")
  @NonNull
  private AccountActivityType type;

  @JsonProperty("status")
  @NonNull
  private AccountActivityStatus status;

  @JsonProperty("user")
  @NonNull
  private LedgerUser user;

  @JsonProperty("hold")
  private LedgerHoldInfo hold;

  @JsonProperty("account")
  private LedgerAccount account;

  @JsonProperty("referenceAccount")
  private LedgerAccount referenceAccount;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;

  // the amount we received from Stripe or a copy of amount otherwise
  @JsonProperty("requestedAmount")
  @NonNull
  private Amount requestedAmount;

  @JsonProperty("receipt")
  private ReceiptDetails receipt;

  @JsonProperty("notes")
  private String notes;

  @JsonProperty("expenseDetails")
  private ExpenseDetails expenseDetails;

  @JsonProperty("syncStatus")
  @NonNull
  private AccountActivityIntegrationSyncStatus syncStatus;

  @JsonProperty("lastSyncTime")
  private OffsetDateTime lastSynctime;

  @JsonProperty("declineDetails")
  private DeclineDetails declineDetails;

  @JsonProperty("paymentDetails")
  private PaymentDetails paymentDetails;

  @JsonProperty("statementDescriptor")
  private String statementDescriptor;

  @JsonIgnore private AccountActivity accountActivity;

  public static LedgerActivityResponse of(AccountActivity accountActivity) {
    LedgerHoldInfo holdInfo = LedgerHoldInfo.of(accountActivity.getHold());

    final LedgerUser ledgerUser;
    LedgerAccount account;
    LedgerAccount referenceAccount;

    switch (accountActivity.getType()) {
      case FEE -> {
        ledgerUser = LedgerUser.SYSTEM_USER;
        account = null;
        referenceAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
      }
      case MANUAL -> {
        ledgerUser = new LedgerUser(accountActivity.getUser());
        // we don't have any manual activities so far so mapping logic might be different
        account = LedgerAllocationAccount.of(accountActivity.getAllocation());
        referenceAccount = null;
      }
      case REALLOCATE -> {
        ledgerUser = new LedgerUser(accountActivity.getUser());
        account = LedgerAllocationAccount.of(accountActivity.getAllocation());
        referenceAccount = LedgerAllocationAccount.of(accountActivity.getFlipAllocation());
      }
      case BANK_DEPOSIT_STRIPE, BANK_WITHDRAWAL -> {
        ledgerUser = new LedgerUser(accountActivity.getUser());
        account = LedgerAllocationAccount.of(accountActivity.getAllocation());
        referenceAccount = LedgerBankAccount.of(accountActivity.getBankAccount());
      }
      case BANK_DEPOSIT_RETURN, BANK_DEPOSIT_ACH, BANK_DEPOSIT_WIRE, BANK_WITHDRAWAL_RETURN -> {
        ledgerUser = LedgerUser.EXTERNAL_USER;
        account = LedgerAllocationAccount.of(accountActivity.getAllocation());
        referenceAccount = LedgerBankAccount.of(accountActivity.getBankAccount());
      }
      case NETWORK_AUTHORIZATION, NETWORK_CAPTURE, NETWORK_REFUND -> {
        ledgerUser = new LedgerUser(accountActivity.getUser());
        account =
            LedgerCardAccount.of(
                accountActivity.getAllocation().getName(), accountActivity.getCard());
        referenceAccount = new LedgerMerchantAccount(accountActivity.getMerchant());
      }
      case CARD_FUND_RETURN -> {
        ledgerUser = LedgerUser.EXTERNAL_USER;
        account = null;
        referenceAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
      }
      default -> {
        ledgerUser = LedgerUser.SYSTEM_USER;
        account = null;
        referenceAccount = null;
      }
    }

    LedgerActivityResponse response =
        new LedgerActivityResponse(
            accountActivity.getId(),
            accountActivity.getActivityTime(),
            accountActivity.getType(),
            accountActivity.getStatus(),
            ledgerUser,
            accountActivity.getAmount(),
            accountActivity.getRequestedAmount(),
            accountActivity.getIntegrationSyncStatus());

    response.setHold(holdInfo);
    response.setAccount(account);
    response.setReferenceAccount(referenceAccount);
    response.setReceipt(ReceiptDetails.toReceiptDetails(accountActivity.getReceipt()));
    response.setNotes(accountActivity.getNotes());
    response.setExpenseDetails(
        ExpenseDetails.toExpenseDetails(accountActivity.getExpenseDetails()));
    response.setLastSynctime(accountActivity.getLastSyncTime());

    if (accountActivity.getMerchant() != null) {
      response.setStatementDescriptor(accountActivity.getMerchant().getStatementDescriptor());
    }

    if (CollectionUtils.isNotEmpty(accountActivity.getDeclineDetails())) {
      response.setDeclineDetails(accountActivity.getDeclineDetails().get(0));
    }

    response.setAccountActivity(accountActivity);
    response.setPaymentDetails(PaymentDetails.from(accountActivity.getPaymentDetails()));

    return response;
  }
}
