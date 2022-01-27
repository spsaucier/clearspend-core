package com.clearspend.capital.controller.nonprod.type.networkmessage;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.network.NetworkMessageId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class NetworkMessageRequest {

  @JsonProperty("cardId")
  TypedId<CardId> cardId;

  @JsonProperty("networkMessageType")
  NetworkMessageType networkMessageType;

  @JsonProperty("amount")
  Amount amount;

  @JsonProperty("priorNetworkMessageId")
  TypedId<NetworkMessageId> priorNetworkMessageId;
}
