package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodatPushData {
  @JsonProperty("id")
  @NonNull
  private String id;

  @JsonProperty("supplierName")
  private String supplierName;
}
