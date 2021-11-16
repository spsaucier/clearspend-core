package com.tranwall.capital.client.i2c.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.CardNumber;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddCardResponse extends BaseI2CResponse {

  @JsonProperty("newCardNumber")
  private CardNumber cardNumber;

  @JsonProperty("referenceId")
  private String i2cCardRef;
}
