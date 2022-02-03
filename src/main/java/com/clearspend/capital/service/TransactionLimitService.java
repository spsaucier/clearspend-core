package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.MccGroupId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionChannel;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.service.type.CardAllocationSpendingDaily;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
  private final AdjustmentService adjustmentService;
  private final AccountActivityRepository accountActivityRepository;

  @Transactional
  public TransactionLimit initializeAllocationSpendLimit(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.ALLOCATION,
            allocationId.toUuid(),
            Map.of(Currency.USD, new HashMap<>()),
            new ArrayList<>(),
            new HashSet<>()));
  }

  public TransactionLimit retrieveSpendLimit(
      TypedId<BusinessId> businessId, TransactionLimitType type, UUID ownerId) {
    return transactionLimitRepository
        .findByBusinessIdAndTypeAndOwnerId(businessId, type, ownerId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.SPEND_LIMIT, businessId, type, ownerId));
  }

  private TransactionLimit duplicateSpendLimit(
      TypedId<BusinessId> businessId, UUID ownerId, TransactionLimit existingTransactionLimit) {
    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.CARD,
            ownerId,
            existingTransactionLimit.getLimits(),
            existingTransactionLimit.getDisabledMccGroups(),
            existingTransactionLimit.getDisabledTransactionChannels()));
  }

  @Transactional
  public TransactionLimit createAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.ALLOCATION,
            allocationId.toUuid(),
            transactionLimits,
            disabledMccGroups,
            disabledTransactionChannels));
  }

  @Transactional
  public TransactionLimit createCardSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.CARD,
            cardId.toUuid(),
            transactionLimits,
            disabledMccGroups,
            disabledTransactionChannels));
  }

  @Transactional
  public TransactionLimit updateAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return updateSpendLimit(
        businessId,
        TransactionLimitType.ALLOCATION,
        allocationId.toUuid(),
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);
  }

  @Transactional
  public TransactionLimit updateCardSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    return updateSpendLimit(
        businessId,
        TransactionLimitType.CARD,
        cardId.toUuid(),
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);
  }

  private TransactionLimit updateSpendLimit(
      TypedId<BusinessId> businessId,
      TransactionLimitType limitType,
      UUID ownerId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    TransactionLimit transactionLimit = retrieveSpendLimit(businessId, limitType, ownerId);

    BeanUtils.setNotNull(transactionLimits, transactionLimit::setLimits);
    BeanUtils.setNotNull(disabledMccGroups, transactionLimit::setDisabledMccGroups);
    BeanUtils.setNotNull(
        disabledTransactionChannels, transactionLimit::setDisabledTransactionChannels);

    return transactionLimitRepository.save(transactionLimit);
  }

  public void ensureWithinLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<CardId> cardId,
      Amount amount) {

    CardAllocationSpendingDaily cardAllocationSpendingDaily =
        accountActivityRepository.findCardAllocationSpendingDaily(
            businessId, allocationId, cardId, 30);

    // check card limits
    TransactionLimit cardTransactionLimits =
        retrieveSpendLimit(businessId, TransactionLimitType.CARD, cardId.toUuid());
    withinLimit(
        cardId,
        TransactionLimitType.CARD,
        amount,
        cardAllocationSpendingDaily.getCardSpendings().get(amount.getCurrency()),
        cardTransactionLimits);

    // check allocation limits
    TransactionLimit allocationTransactionLimits =
        retrieveSpendLimit(businessId, TransactionLimitType.ALLOCATION, allocationId.toUuid());
    withinLimit(
        allocationId,
        TransactionLimitType.ALLOCATION,
        amount,
        cardAllocationSpendingDaily.getAllocationSpendings().get(amount.getCurrency()),
        allocationTransactionLimits);
  }

  private void withinLimit(
      TypedId<?> limitOwner,
      TransactionLimitType transactionLimitType,
      Amount amount,
      Map<LocalDate, Amount> totalSpendings,
      TransactionLimit transactionLimit) {
    if (totalSpendings != null && transactionLimit != null && amount.isLessThanZero()) {
      Map<LimitPeriod, BigDecimal> limits =
          transactionLimit
              .getLimits()
              .getOrDefault(amount.getCurrency(), Map.of())
              .getOrDefault(LimitType.PURCHASE, Map.of());
      for (Entry<LimitPeriod, BigDecimal> limit : limits.entrySet()) {
        LocalDate startDate = LocalDate.now().minusDays(limit.getKey().getDuration().toDays());

        BigDecimal usage =
            totalSpendings.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(startDate))
                .map(e -> e.getValue().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (limit.getValue().add(amount.getAmount()).add(usage).compareTo(BigDecimal.ZERO) < 0) {
          throw new LimitViolationException(
              limitOwner, transactionLimitType, LimitType.PURCHASE, limit.getKey(), amount);
        }
      }
    }
  }
}
