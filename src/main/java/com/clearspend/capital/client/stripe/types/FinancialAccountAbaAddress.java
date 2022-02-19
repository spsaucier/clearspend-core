package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialAccountAbaAddress {

  @JsonProperty("account_number_last4")
  private String accountNumberLast4;

  @JsonProperty("account_number")
  private String accountNumber;

  @JsonProperty("routing_number")
  private String routingNumber;

  @JsonProperty("bank_name")
  private String bankName;
}
