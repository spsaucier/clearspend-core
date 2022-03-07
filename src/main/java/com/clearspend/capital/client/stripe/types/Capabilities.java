package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Capabilities {

  @JsonProperty("card_issuing")
  String cardIssuing;

  @JsonProperty("card_payments")
  String cardPayments;

  @JsonProperty("transfers")
  String transfers;

  @JsonProperty("treasury")
  String treasury;

  @JsonProperty("us_bank_account_ach_payments")
  String usBankAccountAchPayments;
}
