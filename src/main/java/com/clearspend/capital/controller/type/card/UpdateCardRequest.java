package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.MccGroupId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.enums.TransactionChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
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