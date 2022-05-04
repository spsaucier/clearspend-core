package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.service.type.MerchantChartData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class MerchantInfo {

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

  @JsonProperty("merchantLogoUrl")
  @NonNull
  private String merchantLogoUrl;

  public MerchantInfo(MerchantChartData merchant) {
    this.merchantCategoryCode = merchant.getCategoryCode();
    this.merchantNumber = merchant.getNumber();
    this.name = merchant.getName();
    this.type = merchant.getMerchantType();
    this.merchantLogoUrl = merchant.getLogoUrl() != null ? merchant.getLogoUrl() : "";
  }
}
