package com.tranwall.capital.service;

import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.TransactionLimit;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LimitPeriod;
import com.tranwall.capital.data.model.enums.LimitType;
import com.tranwall.capital.data.model.enums.TransactionLimitType;
import com.tranwall.capital.data.repository.TransactionLimitRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitService {

  private final TransactionLimitRepository transactionLimitRepository;

  @Transactional
  public TransactionLimit initializeAllocationSpendLimit(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {
    Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits = new HashMap<>();

    HashMap<LimitPeriod, BigDecimal> allocationDurationMap = new HashMap<>();
    allocationDurationMap.put(LimitPeriod.DAILY, BigDecimal.valueOf(1000));
    allocationDurationMap.put(LimitPeriod.MONTHLY, BigDecimal.valueOf(30000));

    Map<LimitType, Map<LimitPeriod, BigDecimal>> limitTypeMap = new HashMap<>();
    limitTypeMap.put(LimitType.PURCHASE, allocationDurationMap);

    limits.put(Currency.USD, limitTypeMap);

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId, TransactionLimitType.ALLOCATION, allocationId.toUuid(), limits));
  }

  @Transactional
  public TransactionLimit initializeCardSpendLimit(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId, TypedId<CardId> cardId) {
    return duplicateSpendLimit(
        businessId,
        cardId.toUuid(),
        retrieveSpendLimit(businessId, TransactionLimitType.ALLOCATION, allocationId.toUuid()));
  }

  public TransactionLimit retrieveSpendLimit(
      TypedId<BusinessId> businessId, TransactionLimitType type, UUID ownerId) {
    return transactionLimitRepository
        .findByBusinessIdAndTypeAndOwnerId(businessId, type, ownerId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.SPEND_LIMIT, businessId, type, ownerId));
  }

  public List<TransactionLimit> retrieveSpendLimits(
      TypedId<BusinessId> businessId, TransactionLimitType type, List<UUID> ownerIds) {
    return transactionLimitRepository.findByBusinessIdAndTypeAndOwnerIdIn(
        businessId, type, ownerIds);
  }

  private TransactionLimit duplicateSpendLimit(
      TypedId<BusinessId> businessId, UUID ownerId, TransactionLimit existingTransactionLimit) {
    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId, TransactionLimitType.CARD, ownerId, existingTransactionLimit.getLimits()));
  }
}
