package com.tranwall.capital.controller.nonprod.type.networkmessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.push.controller.type.EventNotificationAdvanceRequest;
import com.tranwall.capital.controller.type.Amount;
import lombok.Value;

@Value
public class NetworkMessageRequest {

  @JsonProperty("cardNumber")
  String cardNumber;

  @JsonProperty("mti")
  String mti;

  @JsonProperty("amount")
  Amount amount;

  @JsonProperty("merchantCategoryCode")
  Integer merchantCategoryCode;

  @JsonProperty("request")
  EventNotificationAdvanceRequest request;
}
