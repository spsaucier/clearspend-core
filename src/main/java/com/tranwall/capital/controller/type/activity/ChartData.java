package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChartData {

  @JsonProperty("name")
  private String name;

  @JsonProperty("amount")
  private BigDecimal amount;
}
