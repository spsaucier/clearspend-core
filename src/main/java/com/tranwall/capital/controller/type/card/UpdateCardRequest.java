package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.MccGroupId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.card.limits.CurrencyLimit;
import com.tranwall.capital.data.model.enums.TransactionChannel;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateCardRequest {

  @JsonProperty("limits")
  private List<CurrencyLimit> limits;

  @JsonProperty("disabledMccGroups")
  private List<TypedId<MccGroupId>> disabledMccGroups;

  @JsonProperty("disabledTransactionChannels")
  Set<TransactionChannel> disabledTransactionChannels;
}
