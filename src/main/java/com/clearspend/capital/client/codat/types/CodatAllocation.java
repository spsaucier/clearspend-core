package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodatAllocation {
  @JsonProperty("currency")
  @NonNull
  private String currency;

  @JsonProperty("allocatedOnDate")
  @NonNull
  private OffsetDateTime allocatedOnDate;
}
