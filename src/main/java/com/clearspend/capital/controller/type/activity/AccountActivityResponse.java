package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.common.CardInfo;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@SuppressWarnings("MissingSummary")
@Builder
@AllArgsConstructor
@Getter
public class AccountActivityResponse {

  @JsonProperty("accountActivityId")
  @NonNull
  private TypedId<AccountActivityId> accountActivityId;

  @JsonProperty("activityTime")
  @NonNull
  private OffsetDateTime activityTime;

  @JsonProperty("accountName")
  private String accountName;

  @JsonProperty("card")
  private CardInfo card;

  @JsonProperty("merchant")
  private Merchant merchant;

  @JsonProperty("type")
  @NonNull
  private AccountActivityType type;

  @JsonProperty("status")
  private AccountActivityStatus status;

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

  public AccountActivityResponse(@NonNull AccountActivity accountActivity) {
    this.accountActivityId = accountActivity.getId();
    this.activityTime = accountActivity.getActivityTime();
    this.accountName = accountActivity.getAllocation().getName();
    this.card =
        CardInfo.toCardInfo(accountActivity.getAllocation().getName(), accountActivity.getCard());
    this.receipt = ReceiptDetails.toReceiptDetails(accountActivity.getReceipt());
    this.merchant = Merchant.toMerchant(accountActivity.getMerchant());
    this.type = accountActivity.getType();
    this.status = accountActivity.getStatus();
    this.amount = accountActivity.getAmount();
    this.requestedAmount = accountActivity.getRequestedAmount();
    this.notes = accountActivity.getNotes();
    this.expenseDetails = ExpenseDetails.toExpenseDetails(accountActivity.getExpenseDetails());
    this.syncStatus = accountActivity.getIntegrationSyncStatus();
    this.lastSynctime = accountActivity.getLastSyncTime();
  }
}
