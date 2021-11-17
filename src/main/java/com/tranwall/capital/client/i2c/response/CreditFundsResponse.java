package com.tranwall.capital.client.i2c.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditFundsResponse extends BaseI2CResponse {

  @JsonProperty("balance")
  private BigDecimal balance;

  @JsonProperty("ledgerBalance")
  private BigDecimal ledgerBalance;
}
