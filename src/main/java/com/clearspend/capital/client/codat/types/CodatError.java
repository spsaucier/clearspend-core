package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatError {
  @JsonProperty("itemId")
  @NonNull
  private String itemId;

  @JsonProperty("message")
  @NonNull
  private String message;
}
