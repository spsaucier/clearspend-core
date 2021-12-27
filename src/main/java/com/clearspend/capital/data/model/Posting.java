package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.LedgerAccountId;
import com.clearspend.capital.common.typedid.data.PostingId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
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
import org.hibernate.annotations.Type;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class Posting extends TypedMutable<PostingId> {

  @NonNull
  @JsonIgnore
  @ManyToOne(cascade = CascadeType.ALL)
  private JournalEntry journalEntry;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "ledger_account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<LedgerAccountId> ledgerAccountId;

  @NonNull @Embedded private Amount amount;

  private OffsetDateTime effectiveDate;
}