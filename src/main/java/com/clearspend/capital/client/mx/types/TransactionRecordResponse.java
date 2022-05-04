package com.clearspend.capital.client.mx.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class TransactionRecordResponse {
  @JsonProperty("id")
  private final String id;

  @JsonProperty("description")
  private final String enhancedName;

  @JsonProperty("merchant_guid")
  private final String externalGuid;
}
