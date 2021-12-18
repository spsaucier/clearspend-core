package com.clearspend.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GraphData {

  @JsonProperty("amount")
  private BigDecimal amount;

  @JsonProperty("offsetDateTime")
  private OffsetDateTime offsetDateTime;
}
