package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.common.CardInfo;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.enums.AccountActivityStatus;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

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

  @JsonProperty("receipt")
  private ReceiptDetails receipt;

  public AccountActivityResponse(@NonNull AccountActivity accountActivity) {
    this.accountActivityId = accountActivity.getId();
    this.activityTime = accountActivity.getActivityTime();
    this.accountName = accountActivity.getAllocationName();
    if (accountActivity.getCard() != null && accountActivity.getCard().getCardId() != null) {
      this.card = new CardInfo(accountActivity.getAllocationName(), accountActivity.getCard());
      this.receipt = new ReceiptDetails(accountActivity.getReceipt());
    }
    if (accountActivity.getMerchant() != null) {
      this.merchant = new Merchant(accountActivity.getMerchant());
    }
    this.type = accountActivity.getType();
    this.status = accountActivity.getStatus();
    this.amount = accountActivity.getAmount();
  }
}
