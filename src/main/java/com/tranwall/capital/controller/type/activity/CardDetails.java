package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Card;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class CardDetails {

  @Sensitive
  @JsonProperty("cardId")
  private TypedId<CardId> cardId;

  @Sensitive
  @JsonProperty("cardNumber")
  private String cardNumber;

  public CardDetails(com.tranwall.capital.data.model.embedded.CardDetails card) {
    if (card == null) {
      return;
    }
    cardId = card.getCardId();
  }

  public CardDetails(Card card) {
    if (card == null) {
      return;
    }
    cardId = card.getId();
    cardNumber = card.getCardNumber().getEncrypted();
  }
}
