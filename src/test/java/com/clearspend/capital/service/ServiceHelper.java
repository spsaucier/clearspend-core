package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.service.BusinessService.BusinessAndStripeAccount;
import com.clearspend.capital.service.BusinessService.BusinessRecord;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
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
  private final TransactionLimitService transactionLimitService;
  private final BusinessService businessService;

  public AccountServiceWrapper accountService() {
    return new AccountServiceWrapper(accountService);
  }

  public TransactionLimitServiceWrapper transactionLimitService() {
    return new TransactionLimitServiceWrapper(transactionLimitService);
  }

  public BusinessServiceWrapper businessService() {
    return new BusinessServiceWrapper(businessService);
  }

  @RequiredArgsConstructor
  public static class BusinessServiceWrapper {
    private final BusinessService businessService;

    public BusinessAndStripeAccount createBusiness(
        TypedId<BusinessId> businessId,
        BusinessType businessType,
        String businessEmail,
        ConvertBusinessProspect convertBusinessProspect,
        TosAcceptance tosAcceptance) {
      return businessService.createBusiness(
          businessId, businessType, businessEmail, convertBusinessProspect, tosAcceptance);
    }

    public BusinessRecord getBusiness(TypedId<BusinessId> businessId) {
      return businessService.getBusiness(businessId);
    }
  }

  @RequiredArgsConstructor
  public static class TransactionLimitServiceWrapper {
    private final TransactionLimitService transactionLimitService;

    public TransactionLimit updateCardSpendLimit(
        TypedId<BusinessId> businessId,
        TypedId<CardId> cardId,
        Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
        Set<MccGroup> disabledMccGroups,
        Set<PaymentType> disabledTransactionChannels) {
      return transactionLimitService.updateCardSpendLimit(
          businessId, cardId, transactionLimits, disabledMccGroups, disabledTransactionChannels);
    }

    public TransactionLimit updateAllocationSpendLimit(
        TypedId<BusinessId> businessId,
        TypedId<AllocationId> allocationId,
        Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
        Set<MccGroup> disabledMccGroups,
        Set<PaymentType> disabledPaymentTypes) {
      return transactionLimitService.updateAllocationSpendLimit(
          businessId, allocationId, transactionLimits, disabledMccGroups, disabledPaymentTypes);
    }
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
