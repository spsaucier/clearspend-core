package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Type;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class BankAccountDetails {

  @JoinColumn(referencedColumnName = "id", table = "business_bank_account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessBankAccountId> id;

  @NonNull private String name;

  private String lastFour;

  public static BankAccountDetails of(BusinessBankAccount businessBankAccount) {
    String accountNumber = businessBankAccount.getAccountNumber().getEncrypted();

    return new BankAccountDetails(
        businessBankAccount.getId(),
        businessBankAccount.getName(),
        accountNumber.substring(accountNumber.length() - 4));
  }
}
