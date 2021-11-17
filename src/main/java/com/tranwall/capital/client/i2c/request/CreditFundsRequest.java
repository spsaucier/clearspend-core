package com.tranwall.capital.client.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.Acquirer;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreditFundsRequest {

  @JsonProperty("acquirer")
  private Acquirer acquirer;

  @JsonProperty("card")
  private Card card;

  @JsonProperty("amount")
  private BigDecimal amount;

  @Value
  @Builder
  public static class Card {

    @JsonProperty("referenceId")
    private String i2cAccountRef;
  }
}
