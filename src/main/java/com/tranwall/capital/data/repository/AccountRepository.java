package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  Optional<Account> findByBusinessIdAndTypeAndOwnerIdAndLedgerBalance_Currency(
      UUID businessId, AccountType type, UUID ownerId, Currency currency);

  List<Account> findByBusinessIdAndTypeAndOwnerIdIsInAndLedgerBalance_Currency(
      UUID businessId, AccountType type, List<UUID> ownerId, Currency currency);
}
