package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public enum CodatAccountSubtype {
  @JsonProperty("Fixed Asset")
  FIXED_ASSET("Fixed Asset"),
  @JsonProperty("Other Expense")
  OTHER_EXPENSE("Other Expense");
  @Getter private final String name;

  CodatAccountSubtype(String name) {
    this.name = name;
  }
}
