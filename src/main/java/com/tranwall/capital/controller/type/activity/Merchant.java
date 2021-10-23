package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.enums.MerchantType;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Merchant {

  @JsonProperty("name")
  @NonNull
  private String name;

  @JsonProperty("type")
  @NonNull
  private MerchantType type;
}
