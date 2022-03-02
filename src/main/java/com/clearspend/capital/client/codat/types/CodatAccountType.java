package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public enum CodatAccountType {
  @JsonProperty("Unknown")
  UNKNOWN("Unknown"),
  @JsonProperty("Asset")
  ASSET("Asset"),
  @JsonProperty("Expense")
  EXPENSE("Expense"),
  @JsonProperty("Income")
  INCOME("Income"),
  @JsonProperty("Liability")
  LIABILITY("Liability"),
  @JsonProperty("Equity")
  EQUITY("Equity");

  @Getter private final String name;

  CodatAccountType(String name) {
    this.name = name;
  }
}
