package com.clearspend.capital.client.i2c.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareFundsResponse extends BaseI2CResponse {

  @JsonProperty("fromCardBalance")
  private BigDecimal fromCardBalance;

  @JsonProperty("toCardBalance")
  private BigDecimal toCardBalance;
}
