package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class CodatCreateBankAccountResponse {
  @JsonProperty("validation")
  private CodatValidation validation;

  @JsonProperty("pushOperationKey")
  private String pushOperationKey;

  @JsonProperty("status")
  private String status;
}
