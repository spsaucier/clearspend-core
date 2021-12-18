package com.clearspend.capital.client.i2c.response;

import com.clearspend.capital.client.i2c.CardNumber;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
