package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.Item;
import com.clearspend.capital.controller.type.user.UserData;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepositoryCustom.FilteredCardRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  @NonNull
  @JsonProperty("cardType")
  private CardType cardType;

  public static SearchCardData of(FilteredCardRecord record) {
    return new SearchCardData(
        record.card().getId(),
        record.card().getLastFour(),
        new UserData(record.user()),
        new Item<>(record.allocation().getId(), record.allocation().getName()),
        Amount.of(record.account().getLedgerBalance()),
        record.card().getStatus(),
        record.card().getType());
  }
}
