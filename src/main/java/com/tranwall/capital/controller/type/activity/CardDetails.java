package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CardDetails {

  @Sensitive
  @JsonProperty("cardBin")
  private String number;

  @Sensitive
  @JsonProperty("cardOwner")
  private String owner;
}
