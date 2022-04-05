package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatPushStatusResponse {

  @JsonProperty("data")
  private CodatDataIdStub dataId;

  @JsonProperty("status")
  @NonNull
  private String status;
}
