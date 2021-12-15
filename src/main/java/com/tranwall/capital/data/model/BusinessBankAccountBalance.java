package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.TypedImmutable;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountBalanceId;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
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
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessBankAccountId> businessBankAccountId;

  /** How much the customer can withdraw right now */
  @Embedded private Amount available;

  /** How much the customer has, including monies on hold. */
  @Embedded private Amount current;

  /** For credit accounts, the maximum */
  @Embedded private Amount limit;
}
