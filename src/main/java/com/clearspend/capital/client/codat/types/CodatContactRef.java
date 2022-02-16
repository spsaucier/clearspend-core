package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatContactRef {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("dataType")
  @NonNull
  private String dataType;
}
