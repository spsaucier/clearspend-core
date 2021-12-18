package com.clearspend.capital.client.i2c.request;

import com.clearspend.capital.client.i2c.Acquirer;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SetCardStatusRequest {

  @JsonProperty("acquirer")
  private Acquirer acquirer;

  @JsonProperty("card")
  private Card card;

  @Value
  @Builder
  public static class Card {
    @JsonProperty("referenceId")
    private String referenceId;

    @JsonProperty("statusCode")
    private String statusCode;
  }
}
