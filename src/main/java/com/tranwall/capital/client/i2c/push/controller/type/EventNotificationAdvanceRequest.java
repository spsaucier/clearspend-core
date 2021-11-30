package com.tranwall.capital.client.i2c.push.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventNotificationAdvanceRequest {

  @JsonProperty("Header")
  private I2cHeader i2cHeader;

  @JsonProperty("Transaction")
  private I2cTransaction i2cTransaction;

  @JsonProperty("Card")
  private I2cCard i2cCard;
}
