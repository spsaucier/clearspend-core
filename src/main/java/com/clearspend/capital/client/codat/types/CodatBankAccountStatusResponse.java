package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatBankAccountStatusResponse {
  @JsonProperty("status")
  @NonNull
  private String status;

  @JsonProperty("data")
  @NonNull
  private CodatBankAccount data;
}
