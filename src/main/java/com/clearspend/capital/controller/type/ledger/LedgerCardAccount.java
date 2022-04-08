package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.controller.type.common.CardInfo;
import com.clearspend.capital.data.model.embedded.CardDetails;
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

  public static LedgerCardAccount of(String allocationName, CardDetails cardDetails) {
    if (allocationName == null || cardDetails == null) {
      return null;
    }

    return new LedgerCardAccount(CardInfo.toCardInfo(allocationName, cardDetails));
  }
}
