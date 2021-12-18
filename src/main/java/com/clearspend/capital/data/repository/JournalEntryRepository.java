package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.JournalEntryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository
    extends JpaRepository<JournalEntry, TypedId<JournalEntryId>> {}
