package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
