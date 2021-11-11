package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CardAccountActivityRequest {

  @JsonProperty("type")
  private AccountActivityType type;

  @JsonProperty("from")
  private OffsetDateTime from;

  @JsonProperty("to")
  private OffsetDateTime to;

  @JsonProperty("pageRequest")
  PageRequest pageRequest;
}
