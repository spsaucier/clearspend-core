package com.clearspend.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class GraphData {

  @JsonProperty("from")
  @NonNull
  private OffsetDateTime from;

  @JsonProperty("to")
  @NonNull
  private OffsetDateTime to;

  @JsonProperty("amount")
  @NonNull
  private BigDecimal amount;

  @JsonProperty("count")
  @NonNull
  private BigDecimal count;
}
