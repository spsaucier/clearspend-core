package com.clearspend.capital.client.alloy.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardBusinessRequest {

  @NonNull
  @JsonProperty("business_name")
  private String businessName;

  @NonNull
  @JsonProperty("business_address_line_1")
  private String businessAddressLine1;

  @JsonProperty("business_address_line_2")
  private String businessAddressLine2;

  @NonNull
  @JsonProperty("business_address_city")
  private String businessAddressCity;

  @NonNull
  @JsonProperty("business_address_state")
  private String businessAddressState;

  @NonNull
  @JsonProperty("business_address_postal_code")
  private String businessAddressPostalCode;

  @NonNull
  @JsonProperty("business_federal_ein")
  private String businessFederalEin;

  @NonNull
  @JsonProperty("business_phone_number")
  private String businessPhoneNumber;

  @JsonProperty("business_website")
  private String businessWebsite;
}
