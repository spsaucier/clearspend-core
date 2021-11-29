package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.repository.AccountActivityRepositoryCustom;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

@Builder
@AllArgsConstructor
public class AccountActivityResponse {

  @JsonProperty("accountActivityId")
  @NonNull
  private TypedId<AccountActivityId> accountActivityId;

  @JsonProperty("activityTime")
  @NonNull
  private OffsetDateTime activityTime;

  @JsonProperty("accountName")
  private String accountName;

  @Sensitive
  @JsonProperty("card")
  private CardDetails card;

  @JsonProperty("merchant")
  private Merchant merchant;

  @JsonProperty("type")
  @NonNull
  private AccountActivityType type;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;

  @JsonProperty("receipt")
  private ReceiptDetails receipt;

  public AccountActivityResponse(@NonNull AccountActivity accountActivity) {
    this.accountActivityId = accountActivity.getId();
    this.activityTime = accountActivity.getActivityTime();
    this.accountName = accountActivity.getAllocationName();
    this.card = new CardDetails(accountActivity.getCard());
    this.merchant = new Merchant(accountActivity.getMerchant());
    this.type = accountActivity.getType();
    this.amount = accountActivity.getAmount();
    this.receipt = new ReceiptDetails(accountActivity.getReceipt());
  }

  public AccountActivityResponse(
      @NonNull
          AccountActivityRepositoryCustom.FilteredAccountActivityRecord
              filteredAccountActivityRecord) {
    this.accountActivityId = filteredAccountActivityRecord.accountActivity().getId();
    this.activityTime = filteredAccountActivityRecord.accountActivity().getActivityTime();
    this.accountName = filteredAccountActivityRecord.accountActivity().getAllocationName();
    this.card = new CardDetails(filteredAccountActivityRecord.card());
    this.merchant = new Merchant(filteredAccountActivityRecord.accountActivity().getMerchant());
    this.type = filteredAccountActivityRecord.accountActivity().getType();
    this.amount = filteredAccountActivityRecord.accountActivity().getAmount();
    this.receipt = new ReceiptDetails(filteredAccountActivityRecord.accountActivity().getReceipt());
  }
}
