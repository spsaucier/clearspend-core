package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.fasterxml.jackson.annotation.JsonProperty;
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
