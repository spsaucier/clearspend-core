package com.clearspend.capital.client.mx.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class MxMerchantDetails {

  @JsonProperty("logo_url")
  private String logoUrl;
}
