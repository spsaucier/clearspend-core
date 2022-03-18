package com.clearspend.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilterAmount {

  @JsonProperty("min")
  private BigDecimal min;

  @JsonProperty("max")
  private BigDecimal max;
}
