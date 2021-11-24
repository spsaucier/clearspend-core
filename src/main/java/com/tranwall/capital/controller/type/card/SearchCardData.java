package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.Item;
import com.tranwall.capital.controller.type.user.UserData;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.repository.CardRepositoryCustom.FilteredCardRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class SearchCardData {

  @NonNull
  @JsonProperty("cardId")
  private TypedId<CardId> cardId;

  @NonNull
  @JsonProperty("cardNumber")
  private String cardNumber;

  @NonNull
  @JsonProperty("user")
  private UserData user;

  @NonNull
  @JsonProperty("allocation")
  private Item<TypedId<AllocationId>> allocation;

  @NonNull
  @JsonProperty("balance")
  private Amount balance;

  @NonNull
  @JsonProperty("cardStatus")
  private CardStatus cardStatus;

  public static SearchCardData of(FilteredCardRecord record) {
    return new SearchCardData(
        record.card().getId(),
        record.card().getCardNumber().getEncrypted(),
        new UserData(record.user()),
        new Item<>(record.allocation().getId(), record.allocation().getName()),
        Amount.of(record.account().getLedgerBalance()),
        record.card().getStatus());
  }
}
