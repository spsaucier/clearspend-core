package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@RequiredArgsConstructor
@MappedSuperclass
public class StripeData {

  // identifier of this business (account in stripe terms) at Stripe
  private String accountRef;

  // identifier of the treasury (financial) bank account at Stripe
  private String financialAccountRef;

  @NonNull
  @Enumerated(EnumType.STRING)
  private FinancialAccountState financialAccountState;

  @Sensitive @Embedded private RequiredEncryptedString bankAccountNumber;

  @Sensitive @Embedded private RequiredEncryptedString bankRoutingNumber;

  // on business creation we will collect the ip of the customer, required by Stripe
  @NonNull private String tosAcceptanceIp;
}
