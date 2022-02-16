package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class GetSuppliersResponse {
  @JsonProperty("results")
  @NonNull
  private List<CodatSupplier> results;
}
