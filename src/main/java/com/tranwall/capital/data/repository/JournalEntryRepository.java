package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.JournalEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {}
