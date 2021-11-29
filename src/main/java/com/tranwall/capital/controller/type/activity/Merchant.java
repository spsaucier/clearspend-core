package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.embedded.MerchantDetails;
import com.tranwall.capital.data.model.enums.MerchantType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class Merchant {

  @JsonProperty("name")
  @NonNull
  private String name;

  @JsonProperty("type")
  @NonNull
  private MerchantType type;

  @JsonProperty("merchantNumber")
  @NonNull
  private String merchantNumber;

  @JsonProperty("merchantCategoryCode")
  @NonNull
  private Integer merchantCategoryCode;

  public Merchant(MerchantDetails merchant) {
    if (merchant == null) {
      return;
    }
    name = merchant.getName();
    type = merchant.getType();
    merchantCategoryCode = merchant.getMerchantCategoryCode();
    merchantNumber = merchant.getMerchantNumber();
  }
}
