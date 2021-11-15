package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CardDetails {

  @Sensitive
  @JsonProperty("cardId")
  private TypedId<CardId> cardId;

  public CardDetails(com.tranwall.capital.data.model.embedded.CardDetails card) {
    if (card == null) {
      return;
    }

    cardId = card.getCardId();
  }
}
