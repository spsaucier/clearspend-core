package com.tranwall.capital.client.alloy.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardIndividualRequest {

  @NonNull
  @JsonProperty("name_first")
  private final String nameFirst;

  @NonNull
  @JsonProperty("name_last")
  private final String nameLast;

  @NonNull
  @JsonProperty("email_address")
  private final String emailAddress;

  @NonNull
  @JsonProperty("address_line_1")
  private final String addressLine1;

  @NonNull
  @JsonProperty("address_city")
  private final String addressCity;

  @NonNull
  @JsonProperty("address_state")
  private final String addressState;

  @NonNull
  @JsonProperty("address_postal_code")
  private final String addressPostalCode;

  @NonNull
  @JsonProperty("address_country_code")
  private final String addressCountryCode;

  @NonNull
  @JsonProperty("phone_number")
  private final String phoneNumber;

  @NonNull
  @JsonProperty("birth_date")
  private final LocalDate birthDate;
}
