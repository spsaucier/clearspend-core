package com.tranwall.data.repository;

import com.tranwall.data.model.JournalEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {}
