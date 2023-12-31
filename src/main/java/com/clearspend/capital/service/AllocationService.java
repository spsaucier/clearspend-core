package com.clearspend.capital.service;

import static com.clearspend.capital.common.ValidationHelper.ensureMatchingIds;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.allocation.ArchiveAllocationResponse;
import com.clearspend.capital.controller.type.allocation.StopAllCardsRequest;
import com.clearspend.capital.controller.type.allocation.StopAllCardsResponse;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.permissioncheck.annotations.SqlPermissionAPI;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.errorprone.annotations.RestrictedApi;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationService {

  private final AllocationRepository allocationRepository;
  private final AccountRepository accountRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final CardRepository cardRepository;
  private final HoldRepository holdRepository;
  private final TransactionLimitService transactionLimitService;
  private final RetrievalService retrievalService;
  private final CardService cardService;

  public record AllocationRecord(Allocation allocation, Account account) {}

  public record AllocationDetailsRecord(
      Allocation allocation, Account account, TransactionLimit transactionLimit) {}

  public @interface CreatesRootAllocation {

    String reviewer();

    String explanation();
  }

  @Transactional
  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public ArchiveAllocationResponse archiveAllocation(final TypedId<AllocationId> allocationId) {
    final Allocation targetAllocation =
        retrieveAllocation(CurrentUser.getActiveBusinessId(), allocationId);
    if (targetAllocation.isArchived()) {
      throw new InvalidRequestException("Allocation is already archived");
    }

    if (targetAllocation.getParentAllocationId() == null) {
      throw new InvalidRequestException("Cannot archive the root allocation");
    }

    final List<Allocation> children =
        getAllocationChildren(CurrentUser.getActiveBusinessId(), allocationId);
    final List<Allocation> allAllocations = ListUtils.union(List.of(targetAllocation), children);
    final Set<TypedId<AllocationId>> allAllocationIds =
        allAllocations.stream().map(Allocation::getId).collect(Collectors.toSet());

    validateArchiveAllocationAllowed(allAllocationIds);

    allAllocations.forEach(allocation -> allocation.setArchived(true));
    allocationRepository.saveAll(allAllocations);
    return new ArchiveAllocationResponse(allAllocationIds);
  }

  private void validateArchiveAllocationAllowed(final Set<TypedId<AllocationId>> allocationIds) {
    if (cardRepository.countNonCancelledCardsForAllocations(allocationIds) > 0) {
      throw new InvalidRequestException(
          "Cannot archive an allocation when it or its children have cards still assigned");
    }

    if (holdRepository.countHoldsWithStatusForAllocations(
            HoldStatus.PLACED, allocationIds, OffsetDateTime.now(ZoneOffset.UTC))
        > 0) {
      throw new InvalidRequestException(
          "Cannot archive an allocation when it or its children have unresolved holds");
    }

    if (accountRepository.countAccountsInAllocationsWithBalanceGreaterThan(
            allocationIds, BigDecimal.ZERO)
        > 0) {
      throw new InvalidRequestException(
          "Cannot archive an allocation when it or its children still are funded");
    }
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

    Allocation allocation = new Allocation(businessId, account.getId(), name);
    allocation.setId(allocationId);

    allocation = allocationRepository.save(allocation);

    transactionLimitService.initializeAllocationSpendLimit(
        allocation.getBusinessId(), allocation.getId());

    return new AllocationRecord(allocation, account);
  }

  @SqlPermissionAPI
  /** READ|CUSTOMER_SERVICE|GLOBAL_READ -- findAllocationsWithPermissions.sql */
  public List<AllocationRecord> getAllocationsForBusiness(final TypedId<BusinessId> businessId) {
    final Business business = retrievalService.retrieveBusiness(businessId, true);
    final TypedId<UserId> userId = CurrentUser.getUserId();
    final Set<String> globalRoles = Optional.ofNullable(CurrentUser.getRoles()).orElse(Set.of());

    return getAllocationRecords(
        business,
        allocationRepository.findByBusinessIdWithSqlPermissions(businessId, userId, globalRoles));
  }

  @Transactional
  @PreAuthorize("hasAllocationPermission(#parentAllocationId, 'MANAGE_FUNDS')")
  public AllocationRecord createAllocation(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<AllocationId> parentAllocationId,
      String name,
      Amount amount,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Boolean disableForeign) {

    // create future allocationId so we can create the account first
    TypedId<AllocationId> allocationId = new TypedId<>();
    Account parentAccount = null;

    Allocation parent = retrieveAllocation(businessId, parentAllocationId);

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

    Allocation allocation = new Allocation(businessId, account.getId(), name);
    allocation.setId(allocationId);
    allocation.setParentAllocationId(parentAllocationId);
    allocation.setAncestorAllocationIds(
        allocationRepository.retrieveAncestorAllocationIds(parentAllocationId));

    allocation = allocationRepository.save(allocation);

    if (parentAccount != null) {

      AccountReallocateFundsRecord reallocateFundsRecord =
          accountService.reallocateFunds(parentAccount.getId(), account.getId(), amount);

      final User user = retrievalService.retrieveUser(CurrentUser.getUserId());

      accountActivityService.recordReallocationAccountActivity(
          parent, allocation, reallocateFundsRecord.reallocateFundsRecord().fromAdjustment(), user);
      accountActivityService.recordReallocationAccountActivity(
          allocation, parent, reallocateFundsRecord.reallocateFundsRecord().toAdjustment(), user);
    }

    transactionLimitService.createAllocationSpendLimit(
        businessId,
        allocationId,
        transactionLimits,
        disabledMccGroups,
        disabledPaymentTypes,
        disableForeign);

    return new AllocationRecord(allocation, account);
  }

  Allocation retrieveAllocation(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {
    Allocation allocation = retrievalService.retrieveAllocation(allocationId);

    if (!allocation.getBusinessId().equals(businessId)) {
      throw new DataAccessViolationException(
          Table.ALLOCATION, allocationId, businessId, allocation.getBusinessId());
    }

    return allocation;
  }

  @PreAuthorize("hasAllocationPermission(#allocationId, 'READ|GLOBAL_READ|CUSTOMER_SERVICE')")
  public Allocation getSingleAllocation(
      final TypedId<BusinessId> businessId, final TypedId<AllocationId> allocationId) {
    return retrieveAllocation(businessId, allocationId);
  }

  // TODO: improve entity retrieval to make a single db call
  @PreAuthorize(
      "hasAllocationPermission(#allocationId, 'READ|GLOBAL_READ|CUSTOMER_SERVICE|APPLICATION')")
  public AllocationDetailsRecord getAllocation(
      Business business, TypedId<AllocationId> allocationId) {
    Allocation allocation = retrieveAllocation(business.getId(), allocationId);

    Account account =
        accountService.retrieveAllocationAccount(
            business.getId(), business.getCurrency(), allocationId);

    return new AllocationDetailsRecord(
        allocation,
        account,
        transactionLimitService.retrieveSpendLimit(
            business.getId(), TransactionLimitType.ALLOCATION, allocationId.toUuid()));
  }

  @Transactional
  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_FUNDS')")
  public void updateAllocation(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      String name,
      TypedId<AllocationId> parentAllocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Boolean disableForeign) {

    Allocation allocation = retrieveAllocation(businessId, allocationId);
    if (allocation.isArchived()) {
      throw new InvalidRequestException("Allocation is archived");
    }

    BeanUtils.setNotEmpty(name, allocation::setName);
    BeanUtils.setNotNull(parentAllocationId, allocation::setParentAllocationId);

    transactionLimitService.updateAllocationSpendLimit(
        businessId,
        allocationId,
        transactionLimits,
        disabledMccGroups,
        disabledPaymentTypes,
        disableForeign);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'READ|GLOBAL_READ|CUSTOMER_SERVICE')")
  public AllocationRecord securedGetRootAllocation(final TypedId<BusinessId> businessId) {
    return getRootAllocation(businessId);
  }

  AllocationRecord getRootAllocation(final TypedId<BusinessId> businessId) {
    final Allocation rootAllocation = retrievalService.retrieveRootAllocation(businessId);
    return new AllocationRecord(
        rootAllocation,
        accountService.retrieveAllocationAccount(businessId, Currency.USD, rootAllocation.getId()));
  }

  @PostFilter("hasPermission(filterObject?.allocation, 'READ|CUSTOMER_SERVICE|GLOBAL_READ')")
  public List<AllocationRecord> getAllocationChildrenRecords(
      Business business, TypedId<AllocationId> allocationId) {
    // Retrieve list of allocations which have the parentAllocationId equal to allocationId
    List<Allocation> allocations;
    if (allocationId == null) {
      allocations =
          allocationRepository.findByBusinessIdAndParentAllocationId(
              business.getId(), retrievalService.retrieveRootAllocation(business.getId()).getId());
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

  private record PhysicalCardIds(Set<TypedId<CardId>> cancelled, Set<TypedId<CardId>> unlinked) {}

  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public StopAllCardsResponse stopAllCards(
      final TypedId<AllocationId> allocationId, final StopAllCardsRequest request) {
    if (retrieveAllocation(CurrentUser.getActiveBusinessId(), allocationId).isArchived()) {
      throw new InvalidRequestException("Allocation is archived");
    }

    final Set<TypedId<CardId>> cancelledVirtualCardIds;
    if (request.cancelVirtualCards()) {
      cancelledVirtualCardIds =
          cardRepository
              .findAllNonCancelledByAllocationIdAndType(allocationId, CardType.VIRTUAL)
              .stream()
              .map(card -> cardService.cancelCard(card, CardStatusReason.CARDHOLDER_REQUESTED))
              .map(Card::getId)
              .collect(Collectors.toSet());
    } else {
      cancelledVirtualCardIds = Set.of();
    }

    final PhysicalCardIds physicalCardIds =
        switch (request.stopPhysicalCardsType()) {
          case CANCEL -> new PhysicalCardIds(
              cardRepository
                  .findAllNonCancelledByAllocationIdAndType(allocationId, CardType.PHYSICAL)
                  .stream()
                  .map(card -> cardService.cancelCard(card, CardStatusReason.CARDHOLDER_REQUESTED))
                  .map(Card::getId)
                  .collect(Collectors.toSet()),
              Set.of());
          case UNLINK -> new PhysicalCardIds(
              Set.of(),
              cardRepository
                  .findAllNonCancelledByAllocationIdAndType(allocationId, CardType.PHYSICAL)
                  .stream()
                  .map(cardService::unlinkCard)
                  .map(CardRecord::card)
                  .map(Card::getId)
                  .collect(Collectors.toSet()));
          default -> new PhysicalCardIds(Set.of(), Set.of());
        };

    final StopAllCardsResponse currentResponse =
        new StopAllCardsResponse(
            SetUtils.union(cancelledVirtualCardIds, physicalCardIds.cancelled()),
            physicalCardIds.unlinked());

    final Stream<StopAllCardsResponse> childResponses;
    if (request.applyToChildAllocations()) {
      childResponses =
          getAllocationChildren(CurrentUser.getActiveBusinessId(), allocationId).stream()
              .map(allocation -> stopAllCards(allocation.getId(), request));
    } else {
      childResponses = Stream.empty();
    }

    return childResponses.reduce(
        currentResponse,
        (res1, res2) ->
            new StopAllCardsResponse(
                SetUtils.union(res1.cancelledCards(), res2.cancelledCards()),
                SetUtils.union(res1.unlinkedCards(), res2.unlinkedCards())));
  }

  private List<Allocation> getAllocationChildren(
      final TypedId<BusinessId> businessId, final TypedId<AllocationId> parentAllocationId) {
    return allocationRepository.findByBusinessIdAndParentAllocationId(
        businessId, parentAllocationId);
  }

  @PostFilter("hasPermission(filterObject?.allocation, 'READ|CUSTOMER_SERVICE|GLOBAL_READ')")
  public List<AllocationRecord> searchBusinessAllocations(Business business) {
    return searchBusinessAllocations(business, null);
  }

  @PostFilter("hasPermission(filterObject?.allocation, 'READ|CUSTOMER_SERVICE|GLOBAL_READ')")
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
  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_FUNDS')")
  public AccountReallocateFundsRecord reallocateAllocationFunds(
      Business business,
      @NonNull TypedId<UserId> userId,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull TypedId<AccountId> accountId,
      @NonNull TypedId<CardId> cardId,
      @NonNull AllocationReallocationType allocationReallocationType,
      @NonNull Amount amount) {
    AllocationDetailsRecord allocationDetailsRecord = getAllocation(business, allocationId);
    if (allocationDetailsRecord.allocation().isArchived()) {
      throw new InvalidRequestException("Allocation is archived");
    }
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
  @PreAuthorize("hasAllocationPermission(#allocationId, 'CUSTOMER_SERVICE_MANAGER')")
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

    Business business = retrievalService.retrieveBusiness(businessId, true);

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
