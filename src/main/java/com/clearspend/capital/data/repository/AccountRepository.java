package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, TypedId<AccountId>> {

  Optional<Account> findByBusinessIdAndTypeAndOwnerIdAndLedgerBalance_Currency(
      TypedId<BusinessId> businessId, AccountType type, UUID ownerId, Currency currency);

  List<Account> findByBusinessIdAndTypeAndOwnerIdIsInAndLedgerBalance_Currency(
      TypedId<BusinessId> businessId, AccountType type, List<UUID> ownerId, Currency currency);

  List<Account> findByIdIn(Set<TypedId<AccountId>> ids);

  // for deleting businesses in tests only
  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
