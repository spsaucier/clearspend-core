package com.clearspend.capital.controller.nonprod.type.networkmessage;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.network.NetworkMessageId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class NetworkMessageResponse {

  @JsonProperty("networkMessageId")
  TypedId<NetworkMessageId> networkMessageId;
}
