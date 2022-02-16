package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CodatLineItem {
  @JsonProperty("unitAmount")
  private double unitAmount;

  @JsonProperty("quantity")
  private double quantity;

  @JsonProperty("accountRef")
  @NonNull
  private CodatAccountRef accountRef;

  @JsonProperty("taxRateRef")
  @NonNull
  private CodatTaxRateRef taxRateRef;
}
