package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedStringWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.BusinessRelated;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@DynamicInsert
@Slf4j
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
public class BusinessBankAccount extends TypedMutable<BusinessBankAccountId>
    implements BusinessRelated {
  // TODO CAP-219 persist the plaid Account ID (as plaidAccountRef)

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @Sensitive private String name;

  @Sensitive @Embedded private EncryptedStringWithHash routingNumber;

  @Sensitive @Embedded private EncryptedStringWithHash accountNumber;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash accessToken;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash plaidAccountRef;

  private String stripeBankAccountRef;

  private String stripeSetupIntentRef;

  @NonNull
  @Type(type = "pgsql_enum")
  @ColumnDefault("'LINKED'")
  @Column(columnDefinition = "AccountLinkStatus")
  @Enumerated(EnumType.STRING)
  private AccountLinkStatus linkStatus;

  @NonNull private Boolean deleted;

  @Sensitive private String bankName;
}
