package com.clearspend.capital.data.repository.ledger;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.ledger.JournalEntryId;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository
    extends JpaRepository<JournalEntry, TypedId<JournalEntryId>> {}
