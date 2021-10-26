package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.LedgerAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.AccountType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;
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
public class Account extends TypedMutable<AccountId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "ledger_account")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<LedgerAccountId> ledgerAccountId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AccountType type;

  @NonNull
  @Column(updatable = false)
  private UUID ownerId;

  @NonNull @Embedded private Amount ledgerBalance;

  @Transient private Amount availableBalance;
  @Transient private List<Hold> holds;

  public void setHolds(List<Hold> holds) {
    this.holds = holds;

    BigDecimal holdAmount =
        holds.stream().map(e -> e.getAmount().getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

    availableBalance = ledgerBalance.add(new Amount(ledgerBalance.getCurrency(), holdAmount));
  }
}
