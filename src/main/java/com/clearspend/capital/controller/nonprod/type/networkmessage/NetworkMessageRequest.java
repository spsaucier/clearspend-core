package com.clearspend.capital.controller.nonprod.type.networkmessage;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.enums.NetworkMessageType;
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
}
