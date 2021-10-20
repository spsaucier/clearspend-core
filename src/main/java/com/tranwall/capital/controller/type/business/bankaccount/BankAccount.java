package com.tranwall.capital.controller.type.business.bankaccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class BankAccount {

  @JsonProperty("businessBankAccountId")
  @NonNull
  private TypedId<BusinessBankAccountId> businessBankAccountId;

  @Sensitive
  @JsonProperty("name")
  private String name;

  @Sensitive
  @JsonProperty("routingNumber")
  private String routingNumber;

  @Sensitive
  @JsonProperty("accountNumber")
  private String accountNumber;
}
