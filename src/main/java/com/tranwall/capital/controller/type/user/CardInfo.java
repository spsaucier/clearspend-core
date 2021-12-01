package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CardInfo {

  @JsonProperty("cardId")
  @NonNull
  private TypedId<CardId> cardId;

  @JsonProperty("lastFour")
  @NonNull
  private String lastFour;
}
