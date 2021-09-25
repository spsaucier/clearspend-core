package com.tranwall.data.model;

import com.tranwall.common.data.model.Amount;
import com.tranwall.common.data.model.Mutable;
import com.tranwall.data.model.enums.AccountType;
import com.tranwall.data.model.enums.KnowYourCustomerStatus;
import java.util.UUID;
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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Slf4j
public class Account extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "program")
  @Column(updatable = false)
  private UUID programId;

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

  @NonNull @Embedded private Amount ledgerBalance;
}
