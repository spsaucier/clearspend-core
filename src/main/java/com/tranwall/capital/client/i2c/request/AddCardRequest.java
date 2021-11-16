package com.tranwall.capital.client.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.Acquirer;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddCardRequest {

  @JsonProperty("acquirer")
  private Acquirer acquirer;

  @JsonProperty("card")
  private Card card;

  @JsonProperty("profile")
  private Profile profile;

  @Value
  @Builder
  public static class Card {

    @JsonProperty("startingNumbers")
    private String startingNumbers;
  }

  @Value
  @Builder
  public static class Profile {

    @JsonProperty("nameOnCard")
    private String nameOnCard;
  }
}
