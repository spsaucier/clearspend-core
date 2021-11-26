package com.tranwall.capital.service;

import com.tranwall.capital.client.i2c.I2Client;
import com.tranwall.capital.client.i2c.response.AddStakeholderResponse;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.AllocationReallocationType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.AllocationRepository;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.CardService.CardRecord;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
  private final I2Client i2Client;

  public record AllocationRecord(Allocation allocation, Account account) {}

  @Transactional
  public AllocationRecord createRootAllocation(TypedId<BusinessId> businessId, String name) {
    // create future allocationId so we can create the account first
    TypedId<AllocationId> allocationId = new TypedId<>();
    // creating the account because the allocation references it
    Account account =
        accountService.createAccount(
            businessId, AccountType.ALLOCATION, allocationId.toUuid(), Currency.USD);

    // create new allocation and set its ID to that which was used for the Account record
    AddStakeholderResponse i2cStakeholder = i2Client.addStakeholder(name);
    Allocation allocation =
        new Allocation(
            businessId,
            account.getId(),
            name,
            i2cStakeholder.getI2cStakeholderRef(),
            i2cStakeholder.getI2cAccountRef());
    allocation.setId(allocationId);

    allocation = allocationRepository.save(allocation);

    transactionLimitService.initializeAllocationSpendLimit(
        allocation.getBusinessId(), allocation.getId());

    return new AllocationRecord(allocation, account);
  }

  @Transactional
  public AllocationRecord createAllocation(
      final TypedId<BusinessId> businessId,
      @NonNull final TypedId<AllocationId> parentAllocationId,
      final String name,
      final Amount amount) {
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
        throw new InsufficientFundsException(
            "Account", parentAccount.getId(), AdjustmentType.REALLOCATE, amount);
      }
    }

    // creating the account because the allocation references it
    Account account =
        accountService.createAccount(
            businessId, AccountType.ALLOCATION, allocationId.toUuid(), amount.getCurrency());

    // create new allocation and set its ID to that which was used for the Account record
    // FIXME(akimov) Restore stakeholder ref as soon as i2c fixes the program configuration
    AddStakeholderResponse i2cStakeholder =
        i2Client.addStakeholder(name); // , parent.getI2cStakeholderRef());

    Allocation allocation =
        new Allocation(
            businessId,
            account.getId(),
            name,
            i2cStakeholder.getI2cStakeholderRef(),
            i2cStakeholder.getI2cAccountRef());
    allocation.setId(allocationId);
    allocation.setParentAllocationId(parentAllocationId);
    allocation.setAncestorAllocationIds(
        allocationRepository.retrieveAncestorAllocationIds(parentAllocationId));

    allocation = allocationRepository.save(allocation);

    transactionLimitService.initializeAllocationSpendLimit(
        allocation.getBusinessId(), allocation.getId());

    if (parentAccount != null) {
      accountService.reallocateFunds(parentAccount.getId(), account.getId(), amount);
    }

    return new AllocationRecord(allocation, account);
  }

  public Allocation retrieveAllocation(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {
    return allocationRepository
        .findByBusinessIdAndId(businessId, allocationId)
        .orElseThrow(() -> new RecordNotFoundException(Table.ALLOCATION, businessId, allocationId));
  }

  public AllocationRecord getAllocation(Business business, TypedId<AllocationId> allocationId) {
    Allocation allocation = retrieveAllocation(business.getId(), allocationId);

    Account account =
        accountService.retrieveAllocationAccount(
            business.getId(), business.getCurrency(), allocationId);

    return new AllocationRecord(allocation, account);
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

    Map<UUID, Account> accountMap = getAllocationAccountMap(business, allocations);

    return allocations.stream()
        .map(e -> new AllocationRecord(e, accountMap.get(e.getId().toUuid())))
        .collect(Collectors.toList());
  }

  public List<AllocationRecord> searchBusinessAllocations(Business business) {
    return searchBusinessAllocations(business, null);
  }

  public List<AllocationRecord> searchBusinessAllocations(Business business, String name) {
    log.info("all allocations: {}", allocationRepository.findAll());
    List<Allocation> allocations =
        StringUtils.isEmpty(name)
            ? allocationRepository.findByBusinessId(business.getId())
            : allocationRepository.findByBusinessIdAndNameIgnoreCaseContaining(
                business.getId(), name);

    log.info("allocations {} {}: {}", business.getId(), name, allocations);
    // if none, return empty list
    if (allocations.size() == 0) {
      return Collections.emptyList();
    }

    Map<UUID, Account> accountMap = getAllocationAccountMap(business, allocations);

    return allocations.stream()
        .map(e -> new AllocationRecord(e, accountMap.get(e.getId().toUuid())))
        .collect(Collectors.toList());
  }

  private Map<UUID, Account> getAllocationAccountMap(
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
    return accounts.stream().collect(Collectors.toMap(Account::getOwnerId, Function.identity()));
  }

  @Transactional
  public AccountReallocateFundsRecord reallocateAllocationFunds(
      Business business,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull TypedId<AccountId> accountId,
      @NonNull TypedId<CardId> cardId,
      @NonNull AllocationReallocationType allocationReallocationType,
      @NonNull Amount amount) {
    AllocationRecord allocationRecord = getAllocation(business, allocationId);
    if (!allocationRecord.account().getId().equals(accountId)) {
      throw new IdMismatchException(
          IdType.ACCOUNT_ID, accountId, allocationRecord.account().getId());
    }

    // TODO: Allocations have a default program - the question is do we need to keep the funding
    // type?
    /*
        Program program = programService.retrieveProgram(allocationRecord.allocation.getProgramId());
        if (program.getFundingType() != FundingType.INDIVIDUAL) {
          throw new TypeMismatchException(FundingType.INDIVIDUAL, program.getFundingType());
        }
    */

    CardRecord card = cardService.getCard(business.getId(), cardId);

    AccountReallocateFundsRecord reallocateFundsRecord;
    switch (allocationReallocationType) {
      case ALLOCATION_TO_CARD -> {
        if (allocationRecord.account.getLedgerBalance().isLessThan(amount)) {
          throw new InsufficientFundsException(
              "Account", allocationRecord.account.getId(), AdjustmentType.REALLOCATE, amount);
        }

        reallocateFundsRecord =
            accountService.reallocateFunds(
                allocationRecord.account.getId(), card.account().getId(), amount);
      }
      case CARD_TO_ALLOCATION -> {
        if (card.account().getLedgerBalance().isLessThan(amount)) {
          throw new InsufficientFundsException(
              "Account", allocationRecord.account.getId(), AdjustmentType.REALLOCATE, amount);
        }

        reallocateFundsRecord =
            accountService.reallocateFunds(
                card.account().getId(), allocationRecord.account.getId(), amount);
      }
      default -> throw new IllegalArgumentException(
          "invalid fundsTransactType " + allocationReallocationType);
    }

    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation,
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment());
    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation, reallocateFundsRecord.reallocateFundsRecord().toAdjustment());

    return reallocateFundsRecord;
  }
}
