package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.Currency;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  List<Account> findByBusinessId(TypedId<BusinessId> businessId);

  List<Account> findByBusinessIdAndTypeAndLedgerBalance_Currency(
      TypedId<BusinessId> businessId, AccountType accountType, Currency currency);

  // for deleting businesses in tests only
  void deleteByBusinessId(TypedId<BusinessId> businessId);

  @Query(
      """
      SELECT COUNT(account)
      FROM Account account
      WHERE account.allocationId IN :allocationIds
      AND account.ledgerBalance.amount > :amount
""")
  long countAccountsInAllocationsWithBalanceGreaterThan(
      @Param("allocationIds") final Collection<TypedId<AllocationId>> allocationIds,
      @Param("amount") final BigDecimal amount);
}
