package com.tranwall.capital.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.Mutable;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
public class Posting extends Mutable {

  @NonNull
  @JsonIgnore
  @ManyToOne(cascade = CascadeType.ALL)
  private JournalEntry journalEntry;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "ledger_account")
  @Column(updatable = false)
  private UUID ledgerAccountId;

  @NonNull @Embedded private Amount amount;

  private OffsetDateTime effectiveDate;
}
