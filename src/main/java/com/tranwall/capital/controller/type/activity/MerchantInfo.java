package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.enums.MerchantType;
import com.tranwall.capital.service.type.MerchantChartData;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantInfo {

  @JsonProperty("name")
  private String name;

  @JsonProperty("type")
  private MerchantType type;

  @JsonProperty("merchantNumber")
  private String merchantNumber;

  @JsonProperty("merchantCategoryCode")
  private Integer merchantCategoryCode;

  public MerchantInfo(MerchantChartData merchant) {
    this.merchantCategoryCode = merchant.getCategoryCode();
    this.merchantNumber = merchant.getNumber();
    this.name = merchant.getName();
    this.type = merchant.getMerchantType();
  }
}
