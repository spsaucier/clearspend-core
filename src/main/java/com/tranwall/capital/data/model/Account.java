package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.Mutable;
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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class Account extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  private UUID businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "ledger_account")
  @Column(updatable = false)
  private UUID ledgerAccountId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AccountType type;

  @NonNull
  @Column(updatable = false)
  private UUID ownerId;

  @NonNull
  @Embedded
  private Amount ledgerBalance;

  @Transient
  private Amount availableBalance;
  @Transient
  private List<Hold> holds;

  public void setHolds(List<Hold> holds) {
    this.holds = holds;
    this.availableBalance.setAmount(
        ledgerBalance
            .getAmount()
            .subtract(
                holds.stream()
                    .map(e -> e.getAmount().getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
  }
}
