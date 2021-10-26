package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.embedded.MerchantDetails;
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

  public Merchant(MerchantDetails merchant) {
    if (merchant == null) {
      return;
    }

    name = merchant.getName();
    type = merchant.getType();
  }
}
