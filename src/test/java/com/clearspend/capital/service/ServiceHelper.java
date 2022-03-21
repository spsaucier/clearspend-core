package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.enums.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * In a number of cases, we are using package-private level access to enforce that a given service
 * class method can only be called from methods in other service classes, and nowhere else. This is
 * useful for cases where a given method should have its access permissions enforced by its callers,
 * rather than by itself.
 *
 * <p>Unfortunately, that causes some issues for accessing these particular methods in certain test
 * classes, especially other helper test classes. This class is a workaround for this, providing the
 * ability to access these package-private methods more broadly within the test scope only.
 */
@RequiredArgsConstructor
@Component
public class ServiceHelper {
  private final AccountService accountService;

  public AccountServiceWrapper accountService() {
    return new AccountServiceWrapper(accountService);
  }

  @RequiredArgsConstructor
  public static class AccountServiceWrapper {
    private final AccountService accountService;

    public Account retrieveAllocationAccount(
        final TypedId<BusinessId> businessId,
        final Currency currency,
        final TypedId<AllocationId> allocationId) {
      return accountService.retrieveAllocationAccount(businessId, currency, allocationId);
    }

    public AccountService.AccountReallocateFundsRecord reallocateFunds(
        final TypedId<AccountId> fromAccountId,
        final TypedId<AccountId> toAccountId,
        final Amount amount) {
      return accountService.reallocateFunds(fromAccountId, toAccountId, amount);
    }

    public Account retrieveRootAllocationAccount(
        final TypedId<BusinessId> businessId,
        final Currency currency,
        final TypedId<AllocationId> allocationId,
        final boolean fetchHolds) {
      return accountService.retrieveRootAllocationAccount(
          businessId, currency, allocationId, fetchHolds);
    }

    public Account retrieveAccountById(
        final TypedId<AccountId> accountId, final boolean fetchHolds) {
      return accountService.retrieveAccountById(accountId, fetchHolds);
    }
  }
}
