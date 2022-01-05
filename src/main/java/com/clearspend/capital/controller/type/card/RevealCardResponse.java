package com.clearspend.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class RevealCardResponse {

  @JsonProperty("externalRef")
  @NonNull
  private String externalRef;

  @JsonProperty("ephemeralKey")
  @NonNull
  private String ephemeralKey;
}
