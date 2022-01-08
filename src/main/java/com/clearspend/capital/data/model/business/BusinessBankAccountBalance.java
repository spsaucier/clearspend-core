package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedImmutable;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountBalanceId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class BusinessBankAccountBalance extends TypedImmutable<BusinessBankAccountBalanceId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business_bank_account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessBankAccountId> businessBankAccountId;

  /** How much the customer can withdraw right now */
  @Embedded private Amount available;

  /** How much the customer has, including monies on hold. */
  @Embedded private Amount current;

  /** For credit accounts, the maximum */
  @Embedded private Amount limit;
}
