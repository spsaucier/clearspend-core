package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class IssueCardResponse {

  @JsonProperty("cardId")
  @NonNull
  @NotNull(message = "cardId required")
  private TypedId<CardId> cardId;

  @JsonProperty("errorMessage")
  @Schema(title = "Error message for any records that failed. Will be null if successful")
  private String errorMessage;
}
