package com.clearspend.capital.client.mx.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class TransactionRecordRequest {

  @JsonProperty("id")
  private final String id;

  @JsonProperty("description")
  private final String merchantName;

  @JsonProperty("merchant_category_code")
  private final Integer categoryCode;
}
