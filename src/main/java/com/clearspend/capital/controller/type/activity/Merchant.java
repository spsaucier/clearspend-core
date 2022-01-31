package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
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

  @JsonProperty("merchantLogoUrl")
  private String logoUrl;

  @JsonProperty("merchantLatitude")
  private BigDecimal latitude;

  @JsonProperty("merchantLongitude")
  private BigDecimal longitude;

  public static Merchant toMerchant(MerchantDetails in) {
    if (in == null) {
      return null;
    }

    return new Merchant(
        in.getName(),
        in.getType(),
        in.getMerchantNumber(),
        in.getMerchantCategoryCode(),
        in.getLogoUrl(),
        in.getLatitude(),
        in.getLongitude());
  }
}
