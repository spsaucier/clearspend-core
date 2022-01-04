package com.clearspend.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CardPaymentDetailsResponse {

  @JsonProperty("number")
  @NonNull
  private String number;

  @JsonProperty("expMonth")
  @NonNull
  private Long expMonth;

  @JsonProperty("expYear")
  @NonNull
  private Long expYear;

  @JsonProperty("cvc")
  @NonNull
  private String cvc;
}
