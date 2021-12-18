package com.clearspend.capital.client.i2c.response;

import com.clearspend.capital.client.i2c.CardStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetCardStatusResponse extends BaseI2CResponse {

  @JsonProperty("status")
  private CardStatus status;
}
