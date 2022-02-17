package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, TypedId<AccountId>> {

  Optional<Account> findByBusinessIdAndTypeAndAllocationIdAndLedgerBalance_Currency(
      TypedId<BusinessId> businessId,
      AccountType type,
      TypedId<AllocationId> allocationId,
      Currency currency);

  List<Account> findByBusinessIdAndTypeAndAllocationIdIsInAndLedgerBalance_Currency(
      TypedId<BusinessId> businessId,
      AccountType type,
      List<TypedId<AllocationId>> allocationIds,
      Currency currency);

  Optional<Account> findByBusinessIdAndCardId(
      TypedId<BusinessId> businessId, TypedId<CardId> cardId);

  List<Account> findByIdIn(Set<TypedId<AccountId>> ids);

  List<Account> findByBusinessIdAndTypeAndLedgerBalance_Currency(
      TypedId<BusinessId> businessId, AccountType accountType, Currency currency);

  // for deleting businesses in tests only
  void deleteByBusinessId(TypedId<BusinessId> businessId);
}
