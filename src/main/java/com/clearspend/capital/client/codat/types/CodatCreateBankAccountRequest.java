package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatCreateBankAccountRequest {
  @JsonProperty("accountName")
  private String accountName;

  @JsonProperty("accountNumber")
  private String accountNumber;

  @JsonProperty("accountType")
  private String accountType;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("institution")
  private String institution;
}
