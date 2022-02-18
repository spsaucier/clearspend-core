package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CodatBankAccountsResponse {
  @JsonProperty("results")
  @NonNull
  private List<CodatBankAccount> results;
}
