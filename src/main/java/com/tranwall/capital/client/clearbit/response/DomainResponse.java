package com.tranwall.capital.client.clearbit.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class DomainResponse {

  @JsonProperty("name")
  private String name;

  @JsonProperty("domain")
  private String domain;

  @JsonProperty("logo")
  private String logo;
}
