package com.clearspend.capital.service;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.MccGroupId;
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
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionChannel;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
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

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final CardService cardService;
  private final TransactionLimitService transactionLimitService;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final EntityManager entityManager;
  private final AllocationRolePermissionsService allocationRolePermissionsService;

  public record AllocationRecord(Allocation allocation, Account account) {}

  public record AllocationDetailsRecord(
      Allocation allocation, Account account, User owner, TransactionLimit transactionLimit) {}

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

  @Transactional
  @PreAuthorize("hasPermission(#parentAllocationId, 'MANAGE_FUNDS')")
  public AllocationRecord createAllocation(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<AllocationId> parentAllocationId,
      String name,
      User user,
      Amount amount,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

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
      accountService.reallocateFunds(parentAccount.getId(), account.getId(), amount);
    }

    transactionLimitService.createAllocationSpendLimit(
        businessId,
        allocationId,
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);

    if (user.getType().equals(UserType.EMPLOYEE)) {
      UserRolesAndPermissions parentRole =
          rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(parentAllocationId);
      EnumSet<AllocationPermission> managerPermissions =
          allocationRolePermissionsService.getAllocationRolePermissions(
              businessId, DefaultRoles.ALLOCATION_MANAGER);
      rolesAndPermissionsService.createUserAllocationRole(
          user,
          allocation,
          parentRole.allocationPermissions().containsAll(managerPermissions)
              ? parentRole.allocationRole()
              : DefaultRoles.ALLOCATION_MANAGER);
    }
    return new AllocationRecord(allocation, account);
  }

  public Allocation retrieveAllocation(
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

  // TODO: improve entity retrieval to make a single db call
  public AllocationDetailsRecord getAllocation(
      Business business, TypedId<AllocationId> allocationId) {
    Allocation allocation = retrieveAllocation(business.getId(), allocationId);

    Account account =
        accountService.retrieveAllocationAccount(
            business.getId(), business.getCurrency(), allocationId);

    return new AllocationDetailsRecord(
        allocation,
        account,
        entityManager.getReference(User.class, allocation.getOwnerId()),
        transactionLimitService.retrieveSpendLimit(
            business.getId(), TransactionLimitType.ALLOCATION, allocationId.toUuid()));
  }

  @Transactional
  public void updateAllocation(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      String name,
      TypedId<AllocationId> parentAllocationId,
      TypedId<UserId> ownerId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    Allocation allocation = retrieveAllocation(businessId, allocationId);

    BeanUtils.setNotEmpty(name, allocation::setName);
    BeanUtils.setNotNull(parentAllocationId, allocation::setParentAllocationId);
    BeanUtils.setNotNull(ownerId, allocation::setOwnerId);

    transactionLimitService.updateAllocationSpendLimit(
        businessId,
        allocationId,
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);
  }

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

  public List<AllocationRecord> searchBusinessAllocations(Business business, String name) {
    List<Allocation> allocations =
        StringUtils.isEmpty(name)
            ? allocationRepository.findByBusinessId(business.getId())
            : allocationRepository.findByBusinessIdAndNameIgnoreCaseContaining(
                business.getId(), name);

    // if none, return empty list
    if (allocations.size() == 0) {
      return Collections.emptyList();
    }

    Map<TypedId<AllocationId>, Account> accountMap = getAllocationAccountMap(business, allocations);

    return allocations.stream()
        .map(e -> new AllocationRecord(e, accountMap.get(e.getId())))
        .collect(Collectors.toList());
  }

  private Map<TypedId<AllocationId>, Account> getAllocationAccountMap(
      Business business, List<Allocation> allocations) {
    // get list of accounts to go with the allocations and put into map to make the response easier
    // to create. Expect count to be equal
    List<TypedId<AllocationId>> allocationIds =
        allocations.stream().map(TypedMutable::getId).collect(Collectors.toList());
    List<Account> accounts =
        accountService.retrieveAllocationAccounts(
            business.getId(), business.getCurrency(), allocationIds);
    if (allocations.size() != accounts.size()) {
      throw new IllegalStateException("allocation vs account count mismatch");
    }
    return accounts.stream()
        .collect(Collectors.toMap(Account::getAllocationId, Function.identity()));
  }

  @Transactional
  public AccountReallocateFundsRecord reallocateAllocationFunds(
      Business business,
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

    CardDetailsRecord cardDetailsRecord = cardService.getCard(business.getId(), cardId);

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

    accountActivityService.recordReallocationAccountActivity(
        allocationDetailsRecord.allocation,
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment());
    accountActivityService.recordReallocationAccountActivity(
        allocationDetailsRecord.allocation,
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment());

    return reallocateFundsRecord;
  }
}
