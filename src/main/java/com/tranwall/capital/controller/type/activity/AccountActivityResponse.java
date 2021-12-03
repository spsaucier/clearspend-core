package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.common.CardInfo;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.enums.AccountActivityType;
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

  @JsonProperty("card")
  private CardInfo card;

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
    this.card =
        accountActivity.getCard() != null
            ? new CardInfo(
                accountActivity.getCard().getCardId(),
                accountActivity.getCard().getLastFour(),
                accountActivity.getAllocationName())
            : null;
    this.merchant = new Merchant(accountActivity.getMerchant());
    this.type = accountActivity.getType();
    this.amount = accountActivity.getAmount();
    this.receipt = new ReceiptDetails(accountActivity.getReceipt());
  }
}
