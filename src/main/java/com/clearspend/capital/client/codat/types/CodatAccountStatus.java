package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public enum CodatAccountStatus {
  @JsonProperty("Unknown")
  UNKNOWN("Unknown"),
  @JsonProperty("Active")
  ACTIVE("Active"),
  @JsonProperty("Archived")
  ARCHIVED("Archived"),
  @JsonProperty("Pending")
  PENDING("Pending");

  @Getter private final String name;

  CodatAccountStatus(String name) {
    this.name = name;
  }
}
