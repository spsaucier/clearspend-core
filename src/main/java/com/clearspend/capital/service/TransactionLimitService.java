package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.service.type.CardAllocationSpendingDaily;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
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
  TransactionLimit initializeAllocationSpendLimit(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.ALLOCATION,
            allocationId.toUuid(),
            Map.of(Currency.USD, new HashMap<>()),
            new HashSet<>(),
            new HashSet<>()));
  }

  TransactionLimit retrieveSpendLimit(
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
            existingTransactionLimit.getDisabledPaymentTypes()));
  }

  @Transactional
  TransactionLimit createAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.ALLOCATION,
            allocationId.toUuid(),
            transactionLimits,
            disabledMccGroups,
            disabledPaymentTypes));
  }

  @Transactional
  TransactionLimit createCardSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.CARD,
            cardId.toUuid(),
            transactionLimits,
            disabledMccGroups,
            disabledPaymentTypes));
  }

  @Transactional
  TransactionLimit updateAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    return updateSpendLimit(
        businessId,
        TransactionLimitType.ALLOCATION,
        allocationId.toUuid(),
        transactionLimits,
        disabledMccGroups,
        disabledPaymentTypes);
  }

  @Transactional
  TransactionLimit updateCardSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledTransactionChannels) {

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
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    TransactionLimit transactionLimit = retrieveSpendLimit(businessId, limitType, ownerId);

    BeanUtils.setNotNull(transactionLimits, transactionLimit::setLimits);
    BeanUtils.setNotNull(disabledMccGroups, transactionLimit::setDisabledMccGroups);
    BeanUtils.setNotNull(disabledPaymentTypes, transactionLimit::setDisabledPaymentTypes);

    return transactionLimitRepository.save(transactionLimit);
  }

  void ensureWithinLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<CardId> cardId,
      Amount amount,
      Integer mccCode,
      AuthorizationMethod authorizationMethod) {

    MccGroup mccGroup = MccGroup.fromMcc(mccCode);
    PaymentType paymentType = PaymentType.from(authorizationMethod);

    // card mcc groups and payment types
    TransactionLimit cardTransactionLimits =
        retrieveSpendLimit(businessId, TransactionLimitType.CARD, cardId.toUuid());
    if (cardTransactionLimits.getDisabledMccGroups().contains(mccGroup)) {
      throw new LimitViolationException(cardId, TransactionLimitType.CARD, mccGroup);
    }
    if (cardTransactionLimits.getDisabledPaymentTypes().contains(paymentType)) {
      throw new LimitViolationException(cardId, TransactionLimitType.CARD, paymentType);
    }

    // allocation mcc groups and payment types
    TransactionLimit allocationTransactionLimits =
        retrieveSpendLimit(businessId, TransactionLimitType.ALLOCATION, allocationId.toUuid());
    if (allocationTransactionLimits.getDisabledMccGroups().contains(mccGroup)) {
      throw new LimitViolationException(allocationId, TransactionLimitType.ALLOCATION, mccGroup);
    }
    if (allocationTransactionLimits.getDisabledPaymentTypes().contains(paymentType)) {
      throw new LimitViolationException(allocationId, TransactionLimitType.ALLOCATION, paymentType);
    }

    // spend limits
    CardAllocationSpendingDaily cardAllocationSpendingDaily =
        accountActivityRepository.findCardAllocationSpendingDaily(
            businessId, allocationId, cardId, 30);

    // check card limits
    withinLimit(
        cardId,
        TransactionLimitType.CARD,
        amount,
        cardAllocationSpendingDaily.getCardSpendings().getOrDefault(amount.getCurrency(), Map.of()),
        cardTransactionLimits);

    // check allocation limits
    withinLimit(
        allocationId,
        TransactionLimitType.ALLOCATION,
        amount,
        cardAllocationSpendingDaily
            .getAllocationSpendings()
            .getOrDefault(amount.getCurrency(), Map.of()),
        allocationTransactionLimits);
  }

  private void withinLimit(
      TypedId<?> limitOwner,
      TransactionLimitType transactionLimitType,
      Amount amount,
      Map<LocalDate, Amount> totalSpendings,
      TransactionLimit transactionLimit) {
    if (transactionLimit != null && amount.isLessThanZero()) {
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
