package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class BusinessBankAccount extends TypedMutable<BusinessBankAccountId> {
  // TODO CAP-219 persist the plaid Account ID (as plaidAccountRef)

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @Sensitive private String name;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash routingNumber;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash accountNumber;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash accessToken;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash plaidAccountRef;

  private String stripeBankAccountRef;

  private String stripeSetupIntentRef;

  @NonNull private Boolean deleted;
}
