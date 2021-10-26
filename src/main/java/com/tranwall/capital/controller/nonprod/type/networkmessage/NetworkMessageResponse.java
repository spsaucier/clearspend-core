package com.tranwall.capital.controller.nonprod.type.networkmessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.NetworkMessageId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.Value;

@Value
public class NetworkMessageResponse {

  @JsonProperty("networkMessageId")
  TypedId<NetworkMessageId> networkMessageId;
}
