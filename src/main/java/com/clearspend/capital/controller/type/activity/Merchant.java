package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Merchant {

  @JsonProperty("name")
  @NonNull
  private String name;

  @JsonProperty("type")
  @NonNull
  private MerchantType type;

  @JsonProperty("amount")
  private Amount amount;

  @JsonProperty("merchantNumber")
  @NonNull
  private String merchantNumber;

  @JsonProperty("merchantCategoryCode")
  @NonNull
  private Integer merchantCategoryCode;

  @JsonProperty("merchantCategoryGroup")
  @NonNull
  private MccGroup merchantCategoryGroup;

  @JsonProperty("merchantLogoUrl")
  private String logoUrl;

  @JsonProperty("merchantLatitude")
  private BigDecimal latitude;

  @JsonProperty("merchantLongitude")
  private BigDecimal longitude;

  @JsonProperty("merchantCountry")
  private Country country;

  @JsonProperty("codatSupplierName")
  private String codatSupplierName;

  @JsonProperty("codatSupplierId")
  private String codatSupplierId;

  public static Merchant toMerchant(MerchantDetails in) {
    if (in == null) {
      return null;
    }

    return new Merchant(
        in.getName(),
        in.getType(),
        Amount.of(in.getAmount()),
        in.getMerchantNumber(),
        in.getMerchantCategoryCode(),
        in.getMerchantCategoryGroup(),
        in.getLogoUrl(),
        in.getLatitude(),
        in.getLongitude(),
        in.getCountry(),
        in.getCodatSupplierName(),
        in.getCodatSupplierId());
  }
}
