package com.tranwall.capital.controller.type.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.embedded.CardDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardInfo {

  @JsonProperty("cardId")
  @NonNull
  private TypedId<CardId> cardId;

  @JsonProperty("lastFour")
  @NonNull
  private String lastFour;

  @JsonProperty("allocationName")
  @NonNull
  private String allocationName;

  @Sensitive
  @JsonProperty("ownerFirstName")
  @NonNull
  private String ownerFirstName;

  @Sensitive
  @JsonProperty("ownerLastName")
  @NonNull
  private String ownerLastName;

  public CardInfo(String name, CardDetails card) {
    if (card == null || card.getCardId() == null) {
      return;
    }

    cardId = card.getCardId();
    lastFour = card.getLastFour();
    allocationName = name;
    ownerFirstName = card.getOwnerFirstName().getEncrypted();
    ownerLastName = card.getOwnerLastName().getEncrypted();
  }
}
