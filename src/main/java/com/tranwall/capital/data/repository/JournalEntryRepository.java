package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.JournalEntryId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository
    extends JpaRepository<JournalEntry, TypedId<JournalEntryId>> {}
