package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodatSupplier implements Serializable {
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
  private String defaultCurrency;
}
