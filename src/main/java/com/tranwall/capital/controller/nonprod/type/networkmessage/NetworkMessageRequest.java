package com.tranwall.capital.controller.nonprod.type.networkmessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.data.model.enums.NetworkMessageType;
import lombok.Value;

@Value
public class NetworkMessageRequest {

  @JsonProperty("cardId")
  TypedId<CardId> cardId;

  @JsonProperty("networkMessageType")
  NetworkMessageType networkMessageType;

  @JsonProperty("amount")
  Amount amount;
}
