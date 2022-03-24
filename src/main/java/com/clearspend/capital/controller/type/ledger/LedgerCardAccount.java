package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.controller.type.common.CardInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LedgerCardAccount implements LedgerAccount {

  @JsonProperty("cardInfo")
  @NonNull
  private CardInfo cardInfo;

  @Override
  public LedgerAccountType getType() {
    return LedgerAccountType.CARD;
  }
}
