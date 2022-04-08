package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.activity.ReceiptDetails;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
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

  @JsonProperty("sourceAccount")
  private LedgerAccount sourceAccount;

  @JsonProperty("targetAccount")
  private LedgerAccount targetAccount;

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

  public static LedgerActivityResponse of(AccountActivity accountActivity) {
    LedgerHoldInfo holdInfo = LedgerHoldInfo.of(accountActivity.getHold());

    final LedgerUser ledgerUser;
    LedgerAccount sourceAccount;
    LedgerAccount targetAccount;

    switch (accountActivity.getType()) {
      case FEE -> {
        ledgerUser = LedgerUser.SYSTEM_USER;
        sourceAccount = null;
        targetAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
      }
      case MANUAL -> {
        ledgerUser = new LedgerUser(accountActivity.getUser());
        // we don't have any manual activities so far so mapping logic might be different
        sourceAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
        targetAccount = null;
      }
      case REALLOCATE -> {
        ledgerUser = new LedgerUser(accountActivity.getUser());
        if (accountActivity.getAmount().isLessThanZero()) {
          sourceAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
          targetAccount = LedgerAllocationAccount.of(accountActivity.getFlipAllocation());
        } else {
          sourceAccount = LedgerAllocationAccount.of(accountActivity.getFlipAllocation());
          targetAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
        }
      }
      case BANK_DEPOSIT_STRIPE -> {
        ledgerUser =
            holdInfo != null ? LedgerUser.SYSTEM_USER : new LedgerUser(accountActivity.getUser());
        sourceAccount = LedgerBankAccount.of(accountActivity.getBankAccount());
        targetAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
      }
      case BANK_WITHDRAWAL -> {
        ledgerUser = new LedgerUser(accountActivity.getUser());
        sourceAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
        targetAccount = LedgerBankAccount.of(accountActivity.getBankAccount());
      }
      case BANK_DEPOSIT_RETURN, BANK_DEPOSIT_ACH, BANK_DEPOSIT_WIRE -> {
        ledgerUser = LedgerUser.EXTERNAL_USER;
        sourceAccount = LedgerBankAccount.of(accountActivity.getBankAccount());
        targetAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
      }
      case BANK_WITHDRAWAL_RETURN -> {
        ledgerUser = LedgerUser.EXTERNAL_USER;
        sourceAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
        targetAccount = LedgerBankAccount.of(accountActivity.getBankAccount());
      }
      case NETWORK_AUTHORIZATION, NETWORK_CAPTURE -> {
        ledgerUser =
            holdInfo != null ? LedgerUser.SYSTEM_USER : new LedgerUser(accountActivity.getUser());
        sourceAccount =
            LedgerCardAccount.of(
                accountActivity.getAllocation().getName(), accountActivity.getCard());
        targetAccount = new LedgerMerchantAccount(accountActivity.getMerchant());
      }
      case NETWORK_REFUND -> {
        ledgerUser = LedgerUser.SYSTEM_USER;
        sourceAccount = new LedgerMerchantAccount(accountActivity.getMerchant());
        targetAccount =
            LedgerCardAccount.of(
                accountActivity.getAllocation().getName(), accountActivity.getCard());
      }
      case CARD_FUND_RETURN -> {
        ledgerUser = LedgerUser.EXTERNAL_USER;
        sourceAccount = null;
        targetAccount = LedgerAllocationAccount.of(accountActivity.getAllocation());
      }
      default -> {
        ledgerUser = LedgerUser.SYSTEM_USER;
        sourceAccount = null;
        targetAccount = null;
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
    response.setSourceAccount(sourceAccount);
    response.setTargetAccount(targetAccount);
    response.setReceipt(ReceiptDetails.toReceiptDetails(accountActivity.getReceipt()));
    response.setNotes(accountActivity.getNotes());
    response.setExpenseDetails(
        ExpenseDetails.toExpenseDetails(accountActivity.getExpenseDetails()));
    response.setLastSynctime(accountActivity.getLastSyncTime());

    if (CollectionUtils.isNotEmpty(accountActivity.getDeclineDetails())) {
      response.setDeclineDetails(accountActivity.getDeclineDetails().get(0));
    }

    return response;
  }
}
