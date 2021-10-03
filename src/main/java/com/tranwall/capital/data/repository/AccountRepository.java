package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Account;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {}
