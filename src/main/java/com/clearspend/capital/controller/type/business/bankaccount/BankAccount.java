package com.clearspend.capital.controller.type.business.bankaccount;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.fasterxml.jackson.annotation.JsonProperty;
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
