package com.clearspend.capital.controller.type.ledger;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankInfo {

  @NonNull
  @JsonProperty("name")
  private String name;

  @JsonProperty("accountNumberLastFour")
  private String accountNumberLastFour;
}
