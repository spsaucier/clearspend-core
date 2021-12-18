package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.service.type.MerchantChartData;
import com.fasterxml.jackson.annotation.JsonProperty;
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
