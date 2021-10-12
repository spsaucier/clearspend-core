package com.tranwall.capital.controller.type.business.bankaccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class BankAccount {

  @JsonProperty("businessBankAccountId")
  @NonNull
  private UUID businessBankAccountId;

  @Sensitive
  @JsonProperty("name")
  private String name;

  @Sensitive
  @JsonProperty("accountLastFour")
  private String accountLastFour;
}
