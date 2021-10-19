package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardStatusReason;
import com.tranwall.capital.data.model.enums.FundingType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Card {

  @Sensitive
  @JsonProperty("bin")
  @NonNull
  private String bin;

  @JsonProperty("programId")
  @NonNull
  private TypedId<ProgramId> programId;

  @JsonProperty("allocationId")
  @NonNull
  private TypedId<AllocationId> allocationId;

  @JsonProperty("userId")
  @NonNull
  private TypedId<UserId> userId;

  @JsonProperty("accountId")
  @NonNull
  private TypedId<AccountId> accountId;

  @JsonProperty("status")
  @NonNull
  private CardStatus status;

  @JsonProperty("statusReason")
  @NonNull
  private CardStatusReason statusReason;

  @JsonProperty("fundingType")
  @NonNull
  private FundingType fundingType;

  public Card(com.tranwall.capital.data.model.Card card) {
    this.bin = card.getBin();
    this.programId = card.getProgramId();
    this.allocationId = card.getAllocationId();
    this.userId = card.getUserId();
    this.accountId = card.getAccountId();
    this.status = card.getStatus();
    this.statusReason = card.getStatusReason();
    this.fundingType = card.getFundingType();
  }
}
