package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.JournalEntryId;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString.Exclude;
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
public class JournalEntry extends TypedMutable<JournalEntryId> {

  @JoinColumn(referencedColumnName = "id", table = "journal_entry")
  @Column(columnDefinition = "binary(16)")
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<JournalEntryId> reversedJournalEntryId;

  @JoinColumn(referencedColumnName = "id", table = "journal_entry")
  @Column(columnDefinition = "binary(16)")
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<JournalEntryId> reversalJournalEntryId;

  @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @Exclude
  @EqualsAndHashCode.Exclude
  @NonNull
  private List<Posting> postings;
}
