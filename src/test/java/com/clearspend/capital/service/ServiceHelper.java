package com.clearspend.capital.service;

import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.model.PendingStripeTransfer;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.ledger.LedgerAccount;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.clearspend.capital.service.BusinessService.BusinessAndStripeAccount;
import com.clearspend.capital.service.BusinessService.BusinessRecord;
import com.clearspend.capital.service.TransactionLimitService.CardSpendControls;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import io.fusionauth.domain.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
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
  private final BusinessOwnerService businessOwnerService;
  private final BusinessProspectService businessProspectService;
  private final LedgerService ledgerService;
  private final PendingStripeTransferService pendingStripeTransferService;
  private final CodatService codatService;
  private final ChartOfAccountsService chartOfAccountsService;
  private final AllocationService allocationService;
  private final CoreFusionAuthService coreFusionAuthService;

  public AccountServiceWrapper accountService() {
    return new AccountServiceWrapper(accountService);
  }

  public TransactionLimitServiceWrapper transactionLimitService() {
    return new TransactionLimitServiceWrapper(transactionLimitService);
  }

  public BusinessServiceWrapper businessService() {
    return new BusinessServiceWrapper(businessService);
  }

  public BusinessOwnerServiceWrapper businessOwnerService() {
    return new BusinessOwnerServiceWrapper(businessOwnerService);
  }

  public LedgerServiceWrapper ledgerService() {
    return new LedgerServiceWrapper(ledgerService);
  }

  public PendingStripeTransferServiceWrapper pendingStripeTransferService() {
    return new PendingStripeTransferServiceWrapper(pendingStripeTransferService);
  }

  public CodatServiceWrapper codatService() {
    return new CodatServiceWrapper(codatService);
  }

  public ChartOfAccountsServiceWrapper chartOfAccountsService() {
    return new ChartOfAccountsServiceWrapper(chartOfAccountsService);
  }

  public AllocationServiceWrapper allocationService() {
    return new AllocationServiceWrapper(allocationService);
  }

  public CoreFusionAuthServiceWrapper coreFusionAuthService() {
    return new CoreFusionAuthServiceWrapper(coreFusionAuthService);
  }

  @RequiredArgsConstructor
  public static class AllocationServiceWrapper {
    private final AllocationService allocationService;

    public AllocationRecord getRootAllocation(final TypedId<BusinessId> businessId) {
      return allocationService.getRootAllocation(businessId);
    }
  }

  @RequiredArgsConstructor
  public static class ChartOfAccountsServiceWrapper {
    private final ChartOfAccountsService chartOfAccountsService;

    public ChartOfAccounts updateChartOfAccountsForBusiness(
        TypedId<BusinessId> businessId, List<CodatAccountNested> accountNested) {
      return chartOfAccountsService.updateChartOfAccountsForBusiness(businessId, accountNested);
    }
  }

  @RequiredArgsConstructor
  public static class CodatServiceWrapper {
    private final CodatService codatService;

    public List<CodatAccountNested> nestCodatAccounts(List<CodatAccount> accounts) {
      return codatService.nestCodatAccounts(accounts);
    }
  }

  @RequiredArgsConstructor
  public static class PendingStripeTransferServiceWrapper {
    private final PendingStripeTransferService pendingStripeTransferService;

    public List<PendingStripeTransfer> retrievePendingTransfers(
        final TypedId<BusinessId> businessId) {
      return pendingStripeTransferService.retrievePendingTransfers(businessId);
    }
  }

  @RequiredArgsConstructor
  public static class LedgerServiceWrapper {
    private final LedgerService ledgerService;

    public LedgerAccount getOrCreateLedgerAccount(
        final LedgerAccountType type, final Currency currency) {
      return ledgerService.getOrCreateLedgerAccount(type, currency);
    }
  }

  public BusinessProspectServiceWrapper businessProspectService() {
    return new BusinessProspectServiceWrapper(businessProspectService);
  }

  @RequiredArgsConstructor
  public static class BusinessProspectServiceWrapper {
    private final BusinessProspectService businessProspectService;

    public BusinessOwnerAndUserRecord createMainBusinessOwnerAndRepresentative(
        final BusinessOwnerData businessOwnerData, final TosAcceptance tosAcceptance) {
      return businessProspectService.createMainBusinessOwnerAndRepresentative(
          businessOwnerData, tosAcceptance);
    }
  }

  @RequiredArgsConstructor
  public static class BusinessOwnerServiceWrapper {
    private final BusinessOwnerService businessOwnerService;

    public BusinessOwner retrieveBusinessOwner(final TypedId<BusinessOwnerId> businessOwnerId) {
      return businessOwnerService.retrieveBusinessOwner(businessOwnerId);
    }
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

    public TransactionLimit updateCardSpendLimit(final CardSpendControls request) {
      return transactionLimitService.updateCardSpendLimit(request);
    }

    public TransactionLimit updateAllocationSpendLimit(
        TypedId<BusinessId> businessId,
        TypedId<AllocationId> allocationId,
        Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
        Set<MccGroup> disabledMccGroups,
        Set<PaymentType> disabledPaymentTypes,
        Boolean disableForeign) {
      return transactionLimitService.updateAllocationSpendLimit(
          businessId,
          allocationId,
          transactionLimits,
          disabledMccGroups,
          disabledPaymentTypes,
          disableForeign);
    }
  }

  @RequiredArgsConstructor
  public static class AccountServiceWrapper {
    private final AccountService accountService;

    public AdjustmentAndHoldRecord depositFunds(
        final TypedId<BusinessId> businessId,
        final Account rootAllocationAccount,
        final Amount amount,
        final boolean standardHold) {
      return accountService.depositFunds(businessId, rootAllocationAccount, amount, standardHold);
    }

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

  @RequiredArgsConstructor
  public static class CoreFusionAuthServiceWrapper {
    private final CoreFusionAuthService coreFusionAuthService;

    public boolean changeUserRole(
        @NonNull FusionAuthService.RoleChange change,
        @NonNull String fusionAuthUserId,
        @NonNull String changingRole) {
      return coreFusionAuthService.changeUserRole(change, fusionAuthUserId, changingRole);
    }

    public Set<String> getUserRoles(TypedId<UserId> userId) {
      return coreFusionAuthService.getUserRoles(userId);
    }

    public UUID createBusinessOwner(
        TypedId<BusinessId> businessId,
        TypedId<BusinessOwnerId> businessOwnerId,
        String username,
        String password) {
      return coreFusionAuthService.createBusinessOwner(
          businessId, businessOwnerId, username, password);
    }

    public User getUser(com.clearspend.capital.data.model.User user) {
      // This usage comes up in tests repeatedly, but not in the regular code
      return getUser(UUID.fromString(user.getSubjectRef()));
    }

    public User getUser(UUID fusionAuthId) {
      return coreFusionAuthService.getUser(fusionAuthId);
    }
  }
}
