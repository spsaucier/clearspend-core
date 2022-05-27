package com.clearspend.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateCardSpendControlsRequest {
  @NonNull
  @JsonProperty("allocationSpendControls")
  private List<CardAllocationSpendControls> allocationSpendControls;
}
