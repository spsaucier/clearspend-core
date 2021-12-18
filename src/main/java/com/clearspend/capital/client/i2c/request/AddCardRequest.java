package com.clearspend.capital.client.i2c.request;

import com.clearspend.capital.client.i2c.Acquirer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@JsonInclude(Include.NON_NULL)
public class AddCardRequest {

  @NonNull
  @JsonProperty("acquirer")
  private Acquirer acquirer;

  @NonNull
  @JsonProperty("card")
  private Card card;

  @NonNull
  @JsonProperty("profile")
  private Profile profile;

  @Value
  @Builder
  public static class Card {

    @NonNull
    @JsonProperty("startingNumbers")
    private String startingNumbers;
  }

  @Value
  @Builder
  @JsonInclude(Include.NON_NULL)
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

    @NonNull
    @JsonProperty("nameOnCard")
    private String nameOnCard;
  }
}
