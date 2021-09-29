package com.tranwall.data.repository;

import com.tranwall.data.model.LedgerAccount;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {}
