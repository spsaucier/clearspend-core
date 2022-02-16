package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatPaymentAllocationPayment {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("accountRef")
  @NonNull
  private CodatAccountRef accountRef;

  @JsonProperty("currency")
  @NonNull
  private String currency;
}
