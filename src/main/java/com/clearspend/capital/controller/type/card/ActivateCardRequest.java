package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivateCardRequest {

  @NonNull
  @JsonProperty("lastFour")
  @Pattern(regexp = "^\\d{4}$", message = "Last 4 must contain exactly 4 digits")
  private String lastFour;

  @NonNull
  @JsonProperty("statusReason")
  @Schema(example = "CARDHOLDER_REQUESTED")
  private CardStatusReason statusReason;
}
