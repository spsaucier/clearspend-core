package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.error.TypeMismatchException;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.utils.BigDecimalUtils;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import com.tranwall.capital.data.repository.AllocationRepository;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.CardService.CardRecord;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final ProgramService programService;

  public record AllocationRecord(Allocation allocation, Account account) {}

  @Transactional
  public AllocationRecord createAllocation(
      TypedId<ProgramId> programId,
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> parentAllocationId,
      String name,
      Amount amount) {
    // create future allocationId so we can create the account first
    TypedId<AllocationId> allocationId = new TypedId<>();
    Account parentAccount = null;
    if (BigDecimalUtils.isLargerThan(amount.getAmount(), BigDecimal.ZERO)) {

      if (parentAllocationId == null) {
        parentAccount =
            accountService.retrieveBusinessAccount(businessId, amount.getCurrency(), true);
      } else {
        parentAccount =
            accountService.retrieveAllocationAccount(
                businessId, amount.getCurrency(), parentAllocationId);
      }

      if (parentAccount.getLedgerBalance().isSmallerThan(amount)) {
        throw new InsufficientFundsException(
            parentAccount.getId(), AdjustmentType.REALLOCATE, amount);
      }
    }

    // creating the account because the allocation references it
    Account account =
        accountService.createAccount(
            businessId, AccountType.ALLOCATION, allocationId.toUuid(), amount.getCurrency());

    // create new allocation and set its ID to that which was used for the Account record
    Allocation allocation = new Allocation(businessId, programId, account.getId(), name);
    allocation.setId(allocationId);

    if (parentAllocationId != null) {
      allocation.setParentAllocationId(parentAllocationId);
      Allocation parent =
          allocationRepository
              .findById(parentAllocationId)
              .orElseThrow(() -> new RecordNotFoundException(Table.ALLOCATION, parentAllocationId));

      if (!parent.getBusinessId().equals(businessId)) {
        throw new IdMismatchException(IdType.BUSINESS_ID, businessId, parent.getBusinessId());
      }

      allocation.setAncestorAllocationIds(
          allocationRepository.retrieveAncestorAllocationIds(parentAllocationId));
    }

    allocation = allocationRepository.save(allocation);

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

  public List<AllocationRecord> getAllocationChildren(
      Business business, TypedId<AllocationId> allocationId) {
    // Retrieve list of allocations which have the parentAllocationId equal to allocationId
    List<Allocation> allocations;
    if (allocationId == null) {
      allocations =
          allocationRepository.findByBusinessIdAndParentAllocationIdIsNull(business.getId());
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

  public List<AllocationRecord> searchBusinessAllocations(
      Business business, @NonNull @NotNull(message = "name required") String name) {
    log.info("all allocations: {}", allocationRepository.findAll());
    List<Allocation> allocations =
        allocationRepository.findByBusinessIdAndNameIgnoreCaseContaining(business.getId(), name);
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
    Map<UUID, Account> accountMap =
        accounts.stream().collect(Collectors.toMap(Account::getOwnerId, Function.identity()));
    return accountMap;
  }

  @Transactional
  public AccountReallocateFundsRecord reallocateAllocationFunds(
      Business business,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull TypedId<AccountId> accountId,
      @NonNull TypedId<CardId> cardId,
      @NonNull FundsTransactType fundsTransactType,
      @NonNull Amount amount) {
    AllocationRecord allocationRecord = getAllocation(business, allocationId);
    if (!allocationRecord.account().getId().equals(accountId)) {
      throw new IdMismatchException(
          IdType.ACCOUNT_ID, accountId, allocationRecord.account().getId());
    }

    Program program = programService.retrieveProgram(allocationRecord.allocation.getProgramId());
    if (program.getFundingType() != FundingType.INDIVIDUAL) {
      throw new TypeMismatchException(FundingType.INDIVIDUAL, program.getFundingType());
    }

    CardRecord card = cardService.getCard(business.getId(), cardId);

    AccountReallocateFundsRecord reallocateFundsRecord;
    switch (fundsTransactType) {
      case DEPOSIT -> {
        if (allocationRecord.account.getLedgerBalance().isSmallerThan(amount)) {
          throw new InsufficientFundsException(
              allocationRecord.account.getId(), AdjustmentType.REALLOCATE, amount);
        }

        reallocateFundsRecord =
            accountService.reallocateFunds(
                allocationRecord.account.getId(), card.account().getId(), amount);
      }
      case WITHDRAW -> {
        if (card.account().getLedgerBalance().isSmallerThan(amount)) {
          throw new InsufficientFundsException(
              allocationRecord.account.getId(), AdjustmentType.REALLOCATE, amount);
        }

        reallocateFundsRecord =
            accountService.reallocateFunds(
                card.account().getId(), allocationRecord.account.getId(), amount);
      }
      default -> throw new IllegalArgumentException(
          "invalid fundsTransactType " + fundsTransactType);
    }

    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation.getName(),
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment());
    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation.getName(),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment());

    return reallocateFundsRecord;
  }
}
