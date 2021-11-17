package com.tranwall.capital.client.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.Acquirer;
import com.tranwall.capital.client.i2c.Card;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShareFundsRequest {

  @JsonProperty("acquirer")
  private Acquirer acquirer;

  @JsonProperty("cardFrom")
  private Card cardFrom;

  @JsonProperty("cardTo")
  private Card cardTo;

  @JsonProperty("amount")
  private BigDecimal amount;

  @Value
  @Builder
  public static class Card {

    @JsonProperty("referenceId")
    private String i2cAccountRef;
  }
}
