package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.service.CardService.CardRecord;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.NonNull;

public record CardAndAccount(@NonNull Card card, @Nullable Account account) {
  public static CardAndAccount of(@NonNull final CardRecord cardRecord) {
    return of(cardRecord.card(), cardRecord.account());
  }

  public static CardAndAccount of(
      @NonNull com.clearspend.capital.data.model.Card card,
      @Nullable com.clearspend.capital.data.model.Account account) {
    return new CardAndAccount(
        new Card(card), Optional.ofNullable(account).map(Account::of).orElse(null));
  }
}
