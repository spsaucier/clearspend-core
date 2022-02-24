package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class SyncTransactionResponse {
  @JsonProperty("status")
  @NonNull
  private String status;

  @JsonProperty("codatResponse")
  private CodatSyncDirectCostResponse codatResponse;
}
