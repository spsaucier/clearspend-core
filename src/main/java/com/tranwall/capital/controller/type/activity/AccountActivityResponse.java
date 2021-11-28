package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.TypedId;
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

  public AccountActivityResponse(@NonNull AccountActivity accountActivity) {
    accountActivityId = accountActivity.getId();
    activityTime = accountActivity.getActivityTime();
    accountName = accountActivity.getAllocationName();
    card = new CardDetails(accountActivity.getCard());
    merchant = new Merchant(accountActivity.getMerchant());
    type = accountActivity.getType();
    amount = accountActivity.getAmount();
  }
}
