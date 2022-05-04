package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class GetSuppliersResponse {
  @JsonProperty("totalElements")
  private int totalElements;

  @JsonProperty("results")
  @NonNull
  private List<CodatSupplier> results;
}
