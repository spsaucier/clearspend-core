package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatSyncResponse {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("companyId")
  @NonNull
  private String companyId;

  @JsonProperty("dataType")
  @NonNull
  private String dataType;
}
