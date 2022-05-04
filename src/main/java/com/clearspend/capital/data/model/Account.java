package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.error.InvalidStateException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.ledger.LedgerAccountId;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.HoldStatus;
import java.math.BigDecimal;
import java.util.List;
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
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  // ideally this column/field would have a referential constraint pointing to allocation but since
  // allocation has the opposite to account that's not possible
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> allocationId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "ledger_account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<LedgerAccountId> ledgerAccountId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AccountType type;

  @JoinColumn(referencedColumnName = "id", table = "card")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CardId> cardId;

  @NonNull @Embedded private Amount ledgerBalance;

  public void setAvailableBalance(Amount availableBalance) {
    if (availableBalance == null) {
      throw new InvalidStateException(
          Table.ACCOUNT, "Available balance not set on account " + getBusinessId());
    }

    this.availableBalance = availableBalance;
  }

  @Transient private Amount availableBalance;
  @Transient private List<Hold> holds;

  public void setHolds(List<Hold> holds) {
    this.holds = holds;

    recalculateAvailableBalance();

    log.debug(
        "account {}, ledgerBalance: {}, availableBalance: {}",
        this.getId(),
        ledgerBalance,
        availableBalance);
  }

  public void recalculateAvailableBalance() {
    if (holds == null) {
      return;
    }

    BigDecimal holdAmount =
        holds.stream()
            .filter(hold -> hold.getStatus().equals(HoldStatus.PLACED))
            .map(e -> e.getAmount().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    availableBalance = ledgerBalance.add(new Amount(ledgerBalance.getCurrency(), holdAmount));
  }
}
