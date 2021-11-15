package com.tranwall.capital.client.i2c.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tranwall.capital.client.i2c.CardStatus;
import com.tranwall.capital.client.i2c.util.I2CBooleanDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddCardResponse extends BaseI2CResponse {

  @JsonProperty("isBadPinTriesExceeded")
  @JsonDeserialize(using = I2CBooleanDeserializer.class)
  private boolean badPinTriesExceeded;

  @JsonProperty("status")
  private CardStatus status;

  @JsonProperty("referenceId")
  private String referenceId;

  @JsonProperty("cardNumber")
  private String cardNumber;

  public AddCardResponse(String cardNumber, String referenceId) {
    this.referenceId = referenceId;
    this.cardNumber = cardNumber;
  }
}
