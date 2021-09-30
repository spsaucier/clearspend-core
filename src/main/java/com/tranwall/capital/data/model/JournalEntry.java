package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Mutable;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString.Exclude;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Slf4j
public class JournalEntry extends Mutable {

  @JoinColumn(referencedColumnName = "id", table = "journal_entry")
  @Column(columnDefinition = "binary(16)")
  private UUID reversedJournalEntryId;

  @JoinColumn(referencedColumnName = "id", table = "journal_entry")
  @Column(columnDefinition = "binary(16)")
  private UUID reversalJournalEntryId;

  @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @Exclude
  @EqualsAndHashCode.Exclude
  private List<Posting> postings;
}
