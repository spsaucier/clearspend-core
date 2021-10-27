package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Currency;
import java.util.Optional;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class IssueCardRequest {

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

  @JsonProperty("cardLine3")
  @NonNull
  @Size(max = 26)
  private String cardLine3;

  @JsonProperty("cardLine4")
  @Size(max = 26)
  private Optional<String> cardLine4;
}
