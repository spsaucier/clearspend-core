package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.BusinessBankAccount;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessBankAccountRepository extends JpaRepository<BusinessBankAccount, UUID> {}
