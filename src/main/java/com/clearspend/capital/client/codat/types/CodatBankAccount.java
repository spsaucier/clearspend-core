package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CodatBankAccount {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("accountName")
  @NonNull
  private String accountName;
}
