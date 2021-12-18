package com.clearspend.capital.client.i2c.response;

import com.clearspend.capital.client.i2c.CardStatus;
import com.clearspend.capital.client.i2c.util.I2CBooleanDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetCardStatusResponse extends BaseI2CResponse {

  @JsonProperty("isBadPinTriesExceeded")
  @JsonDeserialize(using = I2CBooleanDeserializer.class)
  private boolean badPinTriesExceeded;

  @JsonProperty("status")
  private CardStatus status;
}
