package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class IssueCardRequest {

  @JsonProperty("programId")
  @NonNull
  @NotNull(message = "programId required")
  @Schema(example = "18104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<ProgramId> programId;

  @JsonProperty("allocationId")
  @NonNull
  @NotNull(message = "allocationId required")
  @Schema(example = "28104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("userId")
  @NonNull
  @NotNull(message = "User id required")
  @Schema(example = "38104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<UserId> userId;

  @JsonProperty("currency")
  @NonNull
  @NotNull(message = "Currency required")
  private Currency currency;

  @JsonProperty("cardType")
  @NonNull
  @NotNull(message = "Card Type required")
  @Size(max = 2)
  private Set<CardType> cardType;

  @JsonProperty("isPersonal")
  @NonNull
  @NotNull(message = "isPersonal required")
  private Boolean isPersonal;
}
