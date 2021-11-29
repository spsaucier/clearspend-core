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

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("address")
    private String address;

    @JsonProperty("city")
    private String city;

    @JsonProperty("stateCode")
    private String stateCode;

    @JsonProperty("postalCode")
    private String postalCode;

    @JsonProperty("country")
    private String country;

    @JsonProperty("cellNumber")
    private String cellNumber;

    @JsonProperty("email")
    private String email;

    @JsonProperty("nameOnCard")
    private String nameOnCard;
  }
}
