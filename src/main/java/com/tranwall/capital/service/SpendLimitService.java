package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.SpendLimit;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.SpendLimitType;
import com.tranwall.capital.data.repository.SpendLimitRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpendLimitService {

  private final SpendLimitRepository spendLimitRepository;

  public SpendLimit initializeAllocationSpendLimit(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {
    return spendLimitRepository.save(
        new SpendLimit(
            businessId,
            SpendLimitType.ALLOCATION,
            allocationId.toUuid(),
            Amount.of(Currency.USD, BigDecimal.valueOf(1000)),
            Amount.of(Currency.USD, BigDecimal.valueOf(30000))));
  }

  public SpendLimit initializeCardSpendLimit(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId, TypedId<CardId> cardId) {
    return duplicateSpendLimit(
        businessId,
        cardId.toUuid(),
        retrieveSpendLimit(businessId, SpendLimitType.ALLOCATION, allocationId.toUuid()));
  }

  private SpendLimit retrieveSpendLimit(
      TypedId<BusinessId> businessId, SpendLimitType type, UUID ownerId) {
    return spendLimitRepository
        .findByBusinessIdAndTypeAndOwnerId(businessId, type, ownerId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.SPEND_LIMIT, businessId, type, ownerId));
  }

  private SpendLimit duplicateSpendLimit(
      TypedId<BusinessId> businessId, UUID ownerId, SpendLimit existingSpendLimit) {
    return spendLimitRepository.save(
        new SpendLimit(
            businessId,
            SpendLimitType.CARD,
            ownerId,
            existingSpendLimit.getDailyPurchaseLimit(),
            existingSpendLimit.getMonthlyPurchaseLimit()));
  }
}
