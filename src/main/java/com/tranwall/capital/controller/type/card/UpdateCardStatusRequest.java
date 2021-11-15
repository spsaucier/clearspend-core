package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardStatusReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateCardStatusRequest {

  @JsonProperty("status")
  @Schema(example = "BLOCKED")
  private CardStatus status;

  @JsonProperty("statusReason")
  @Schema(example = "CARDHOLDER_REQUESTED")
  private CardStatusReason statusReason;
}
