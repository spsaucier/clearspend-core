package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.LedgerAccount;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {}
