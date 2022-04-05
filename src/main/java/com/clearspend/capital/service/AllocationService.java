package com.clearspend.capital.service;

import static com.clearspend.capital.common.ValidationHelper.ensureMatchingIds;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.google.errorprone.annotations.RestrictedApi;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationService {

  private final AllocationRepository allocationRepository;
  private final BusinessRepository businessRepository;
  private final UserRepository userRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final CardRepository cardRepository;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final TransactionLimitService transactionLimitService;
  private final RetrievalService retrievalService;

  private final EntityManager entityManager;

  public record AllocationRecord(Allocation allocation, Account account) {}

  public record AllocationDetailsRecord(
      Allocation allocation, Account account, User owner, TransactionLimit transactionLimit) {}

  public @interface CreatesRootAllocation {

    String reviewer();

    String explanation();
  }

  @RestrictedApi(
      explanation = "This only happens during onboarding",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowedOnPath = "test/.*",
      allowlistAnnotations = {CreatesRootAllocation.class})
  @Transactional
  public AllocationRecord createRootAllocation(
      TypedId<BusinessId> businessId, User user, String name) {
    if (!Objects.equals(businessId, user.getBusinessId())) {
      throw new IdMismatchException(IdType.BUSINESS_ID, businessId, user.getBusinessId());
    }

    // create future allocationId so we can create the account first
    TypedId<AllocationId> allocationId = new TypedId<>();
    // creating the account because the allocation references it
    Account account =
        accountService.createAccount(
            businessId, AccountType.ALLOCATION, allocationId, null, Currency.USD);

    Allocation allocation = new Allocation(businessId, account.getId(), user.getId(), name);
    allocation.setId(allocationId);

    allocation = allocationRepository.save(allocation);

    transactionLimitService.initializeAllocationSpendLimit(
        allocation.getBusinessId(), allocation.getId());

    return new AllocationRecord(allocation, account);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'VIEW_OWN')")
  public List<AllocationRecord> getAllocationsForBusiness(final TypedId<BusinessId> businessId) {
    final Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));
    return getAllocationRecords(business, allocationRepository.findByBusinessId(businessId));
  }

  @Transactional
  @PreAuthorize("hasPermission(#parentAllocationId, 'MANAGE_FUNDS')")
  public AllocationRecord createAllocation(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<AllocationId> parentAllocationId,
      String name,
      User user,
      Amount amount,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    // create future allocationId so we can create the account first
    TypedId<AllocationId> allocationId = new TypedId<>();
    Account parentAccount = null;

    Allocation parent =
        allocationRepository
            .findById(parentAllocationId)
            .orElseThrow(() -> new RecordNotFoundException(Table.ALLOCATION, parentAllocationId));

    if (!parent.getBusinessId().equals(businessId)) {
      throw new IdMismatchException(IdType.BUSINESS_ID, businessId, parent.getBusinessId());
    }

    if (amount.isGreaterThanZero()) {
      parentAccount =
          accountService.retrieveAllocationAccount(
              businessId, amount.getCurrency(), parentAllocationId);

      if (parentAccount.getLedgerBalance().isLessThan(amount)) {
        throw new InsufficientFundsException(parentAccount, AdjustmentType.REALLOCATE, amount);
      }
    }

    // creating the account because the allocation references it
    Account account =
        accountService.createAccount(
            businessId, AccountType.ALLOCATION, allocationId, null, amount.getCurrency());

    Allocation allocation = new Allocation(businessId, account.getId(), user.getId(), name);
    allocation.setId(allocationId);
    allocation.setParentAllocationId(parentAllocationId);
    allocation.setAncestorAllocationIds(
        allocationRepository.retrieveAncestorAllocationIds(parentAllocationId));

    allocation = allocationRepository.save(allocation);

    if (parentAccount != null) {

      AccountReallocateFundsRecord reallocateFundsRecord =
          accountService.reallocateFunds(parentAccount.getId(), account.getId(), amount);

      accountActivityService.recordReallocationAccountActivity(
          parent, allocation, reallocateFundsRecord.reallocateFundsRecord().fromAdjustment(), user);
      accountActivityService.recordReallocationAccountActivity(
          allocation, parent, reallocateFundsRecord.reallocateFundsRecord().toAdjustment(), user);
    }

    transactionLimitService.createAllocationSpendLimit(
        businessId, allocationId, transactionLimits, disabledMccGroups, disabledPaymentTypes);

    ensureAllocationOwnerPermissions(user, allocation);

    return new AllocationRecord(allocation, account);
  }

  private void ensureAllocationOwnerPermissions(User user, Allocation allocation) {
    rolesAndPermissionsService.ensureMinimumAllocationPermissions(
        user, allocation, DefaultRoles.ALLOCATION_MANAGER);
  }

  Allocation retrieveAllocation(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {
    Allocation allocation =
        allocationRepository
            .findById(allocationId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.ALLOCATION, businessId, allocationId));

    if (!allocation.getBusinessId().equals(businessId)) {
      throw new DataAccessViolationException(
          Table.ALLOCATION, allocationId, businessId, allocation.getBusinessId());
    }

    return allocation;
  }

  @PreAuthorize(
      "hasAllocationPermission(#allocationId, 'READ') or "
          + "hasGlobalPermission('GLOBAL_READ|CUSTOMER_SERVICE')")
  public Allocation getSingleAllocation(
      final TypedId<BusinessId> businessId, final TypedId<AllocationId> allocationId) {
    return retrieveAllocation(businessId, allocationId);
  }

  // TODO: improve entity retrieval to make a single db call
  @PreAuthorize(
      "hasAllocationPermission(#allocationId, 'READ') or "
          + "hasGlobalPermission('GLOBAL_READ|CUSTOMER_SERVICE')")
  public AllocationDetailsRecord getAllocation(
      Business business, TypedId<AllocationId> allocationId) {
    Allocation allocation = retrieveAllocation(business.getId(), allocationId);

    Account account =
        accountService.retrieveAllocationAccount(
            business.getId(), business.getCurrency(), allocationId);

    return new AllocationDetailsRecord(
        allocation,
        account,
        userRepository
            .findById(allocation.getOwnerId())
            .orElseThrow(() -> new RecordNotFoundException(Table.USER, allocation.getOwnerId())),
        transactionLimitService.retrieveSpendLimit(
            business.getId(), TransactionLimitType.ALLOCATION, allocationId.toUuid()));
  }

  @Transactional
  @PreAuthorize("hasPermission(#allocationId, 'MANAGE_FUNDS')")
  public void updateAllocation(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      String name,
      TypedId<AllocationId> parentAllocationId,
      TypedId<UserId> ownerId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    Allocation allocation = retrieveAllocation(businessId, allocationId);

    BeanUtils.setNotEmpty(name, allocation::setName);
    BeanUtils.setNotNull(parentAllocationId, allocation::setParentAllocationId);
    BeanUtils.setNotNull(ownerId, allocation::setOwnerId);

    transactionLimitService.updateAllocationSpendLimit(
        businessId, allocationId, transactionLimits, disabledMccGroups, disabledPaymentTypes);

    ensureAllocationOwnerPermissions(
        entityManager.getReference(User.class, allocation.getOwnerId()), allocation);
  }

  // TODO: should be uncomment when CAP-442 is implemented - to allow security context creation for
  // webhooks
  // @PreAuthorize("hasPermission(#businessId, 'BusinessId', 'READ|GLOBAL_READ|CUSTOMER_SERVICE')")
  public AllocationRecord getRootAllocation(TypedId<BusinessId> businessId) {
    Allocation rootAllocation =
        allocationRepository.findByBusinessIdAndParentAllocationIdIsNull(businessId);
    if (rootAllocation == null) {
      throw new RecordNotFoundException(Table.ALLOCATION, businessId);
    }
    return new AllocationRecord(
        rootAllocation,
        accountService.retrieveAllocationAccount(businessId, Currency.USD, rootAllocation.getId()));
  }

  @PreAuthorize(
      "hasAllocationPermission(#allocationId, 'READ') or "
          + "hasGlobalPermission('CUSTOMER_SERVICE|GLOBAL_READ')")
  public List<AllocationRecord> getAllocationChildren(
      Business business, TypedId<AllocationId> allocationId) {
    // Retrieve list of allocations which have the parentAllocationId equal to allocationId
    List<Allocation> allocations;
    if (allocationId == null) {
      allocations =
          allocationRepository.findByBusinessIdAndParentAllocationId(
              business.getId(),
              allocationRepository
                  .findByBusinessIdAndParentAllocationIdIsNull(business.getId())
                  .getId());
    } else {
      allocations =
          allocationRepository.findByBusinessIdAndParentAllocationId(
              business.getId(), allocationId);
    }
    // if none, return empty list
    return getAllocationRecords(business, allocations);
  }

  @NonNull
  private List<AllocationRecord> getAllocationRecords(
      Business business, List<Allocation> allocations) {
    if (allocations.size() == 0) {
      return Collections.emptyList();
    }

    Map<TypedId<AllocationId>, Account> accountMap = getAllocationAccountMap(business, allocations);

    return allocations.stream()
        .map(e -> new AllocationRecord(e, accountMap.get(e.getId())))
        .collect(Collectors.toList());
  }

  public List<AllocationRecord> searchBusinessAllocations(Business business) {
    return searchBusinessAllocations(business, null);
  }

  @PreAuthorize("hasPermission(#business, 'READ|CUSTOMER_SERVICE|GLOBAL_READ')")
  public List<AllocationRecord> searchBusinessAllocations(Business business, String name) {
    List<Allocation> allocations =
        StringUtils.isEmpty(name)
            ? allocationRepository.findByBusinessId(business.getId())
            : allocationRepository.findByBusinessIdAndNameIgnoreCaseContaining(
                business.getId(), name);

    // if none, return empty list
    return getAllocationRecords(business, allocations);
  }

  private Map<TypedId<AllocationId>, Account> getAllocationAccountMap(
      Business business, List<Allocation> allocations) {
    // get list of accounts to go with the allocations and put into map to make the response easier
    // to create. Expect count to be equal
    List<TypedId<AllocationId>> allocationIds =
        allocations.stream().map(TypedMutable::getId).collect(Collectors.toList());
    List<Account> accounts =
        accountService.retrieveAllocationAccounts(
            business.getId(), business.getCurrency(), allocationIds, true);
    if (allocations.size() != accounts.size()) {
      throw new IllegalStateException("allocation vs account count mismatch");
    }
    return accounts.stream()
        .collect(Collectors.toMap(Account::getAllocationId, Function.identity()));
  }

  @Transactional
  @PreAuthorize("hasPermission(#allocationId, 'MANAGE_FUNDS')")
  public AccountReallocateFundsRecord reallocateAllocationFunds(
      Business business,
      @NonNull TypedId<UserId> userId,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull TypedId<AccountId> accountId,
      @NonNull TypedId<CardId> cardId,
      @NonNull AllocationReallocationType allocationReallocationType,
      @NonNull Amount amount) {
    AllocationDetailsRecord allocationDetailsRecord = getAllocation(business, allocationId);
    if (!allocationDetailsRecord.account().getId().equals(accountId)) {
      throw new IdMismatchException(
          IdType.ACCOUNT_ID, accountId, allocationDetailsRecord.account().getId());
    }

    CardDetailsRecord cardDetailsRecord =
        cardRepository.findDetailsByBusinessIdAndId(business.getId(), cardId).orElseThrow();

    AccountReallocateFundsRecord reallocateFundsRecord;
    switch (allocationReallocationType) {
      case ALLOCATION_TO_CARD -> {
        if (allocationDetailsRecord.account.getLedgerBalance().isLessThan(amount)) {
          throw new InsufficientFundsException(
              allocationDetailsRecord.account, AdjustmentType.REALLOCATE, amount);
        }

        reallocateFundsRecord =
            accountService.reallocateFunds(
                allocationDetailsRecord.account.getId(),
                cardDetailsRecord.account().getId(),
                amount);
      }
      case CARD_TO_ALLOCATION -> {
        if (cardDetailsRecord.account().getLedgerBalance().isLessThan(amount)) {
          throw new InsufficientFundsException(
              allocationDetailsRecord.account, AdjustmentType.REALLOCATE, amount);
        }

        reallocateFundsRecord =
            accountService.reallocateFunds(
                cardDetailsRecord.account().getId(),
                allocationDetailsRecord.account.getId(),
                amount);
      }
      default -> throw new IllegalArgumentException(
          "invalid fundsTransactType " + allocationReallocationType);
    }

    User user = retrievalService.retrieveUser(business.getId(), userId);

    accountActivityService.recordReallocationAccountActivity(
        allocationDetailsRecord.allocation,
        null,
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment(),
        user);
    accountActivityService.recordReallocationAccountActivity(
        allocationDetailsRecord.allocation,
        null,
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment(),
        user);

    return reallocateFundsRecord;
  }

  @Transactional
  @PreAuthorize("hasPermission(#allocationId, 'CUSTOMER_SERVICE_MANAGER')")
  public AdjustmentRecord updateAllocationBalance(
      @NonNull TypedId<BusinessId> businessId,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull Amount amount,
      @NonNull String notes) {

    Amount maxAmount = Amount.of(amount.getCurrency(), 1000);
    if (amount.abs().isGreaterThan(maxAmount)) {
      throw new InvalidRequestException(
          "Amounts in excess of +/- %s not allowed".formatted(maxAmount));
    }

    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, true, businessId));

    Allocation allocation = retrieveAllocation(business.getId(), allocationId);
    ensureMatchingIds(business.getId(), allocation.getBusinessId());

    Account account =
        accountService.retrieveAllocationAccount(
            business.getId(), business.getCurrency(), allocation.getId());
    ensureMatchingIds(business.getId(), account.getBusinessId());
    ensureMatchingIds(allocation.getId(), account.getAllocationId());

    AdjustmentRecord adjustmentRecord = accountService.manualAdjustment(account, amount);
    accountActivityService.recordManualAdjustmentActivity(
        allocation, adjustmentRecord.adjustment(), notes);

    return adjustmentRecord;
  }
}
