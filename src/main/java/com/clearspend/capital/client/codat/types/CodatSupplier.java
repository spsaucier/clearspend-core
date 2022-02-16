package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatSupplier {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("supplierName")
  @NonNull
  private String supplierName;

  @JsonProperty("status")
  @NonNull
  private String status;

  @JsonProperty("defaultCurrency")
  @NonNull
  private String defaultCurrency;
}
