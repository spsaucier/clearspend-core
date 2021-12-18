package com.clearspend.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CardResponse {

  @JsonProperty("card")
  @NonNull
  @NotNull(message = "card required")
  private Card card;
}
