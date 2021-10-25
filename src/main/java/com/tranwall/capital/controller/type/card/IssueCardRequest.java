package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class IssueCardRequest {

  @Sensitive
  @JsonProperty("bin")
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

  @JsonProperty("currency")
  @NonNull
  private Currency currency;

  @JsonProperty("cardType")
  @NonNull
  private CardType cardType;
}
