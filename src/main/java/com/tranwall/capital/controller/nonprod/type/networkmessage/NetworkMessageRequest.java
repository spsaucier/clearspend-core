package com.tranwall.capital.controller.nonprod.type.networkmessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import java.time.LocalDate;
import lombok.Value;

@Value
public class NetworkMessageRequest {

  @JsonProperty("cardNumber")
  String cardNumber;

  @JsonProperty("expirationDate")
  LocalDate expirationDate;

  @JsonProperty("mti")
  int MTI;

  @JsonProperty("amount")
  Amount amount;
}
