package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.Mutable;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
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
public class Adjustment extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  private UUID businessId;

  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  private UUID allocationId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "account")
  @Column(updatable = false)
  private UUID accountId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "ledger_account")
  @Column(updatable = false)
  private UUID ledgerAccountId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "journal_entry")
  @Column(updatable = false)
  private UUID journalEntryId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "posting")
  @Column(updatable = false)
  private UUID postingId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AdjustmentType type;

  @NonNull private OffsetDateTime effectiveDate;

  @NonNull @Embedded private Amount amount;
}
