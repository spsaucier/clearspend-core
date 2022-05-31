package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.SpendControlViolationException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.card.CardAllocationSpendControls;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.AllocationRelated;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.CardAllocation;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.CardAllocationRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.service.type.CardAllocationSpendingDaily;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitService {

  private final TransactionLimitRepository transactionLimitRepository;
  private final AccountActivityRepository accountActivityRepository;
  private final CardAllocationRepository cardAllocationRepository;

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
            new HashSet<>(),
            false));
  }

  TransactionLimit retrieveSpendLimit(
      TypedId<BusinessId> businessId, TransactionLimitType type, UUID ownerId) {
    return transactionLimitRepository
        .findByBusinessIdAndTypeAndOwnerId(businessId, type, ownerId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.SPEND_LIMIT, businessId, type, ownerId));
  }

  @Transactional
  void removeSpendLimit(
      final TypedId<BusinessId> businessId, final TransactionLimitType type, final UUID ownerId) {
    transactionLimitRepository.deleteByBusinessIdAndTypeAndOwnerId(businessId, type, ownerId);
  }

  @Transactional
  TransactionLimit createAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Boolean disableForeign) {

    return transactionLimitRepository.save(
        new TransactionLimit(
            businessId,
            TransactionLimitType.ALLOCATION,
            allocationId.toUuid(),
            transactionLimits,
            disabledMccGroups,
            disabledPaymentTypes,
            disableForeign));
  }

  @Transactional
  @PreAuthorize("hasPermission(#request, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public TransactionLimit createCardSpendLimit(final CardSpendControls request) {
    final TransactionLimit allocationLimit =
        retrieveSpendLimit(
            request.card().getBusinessId(),
            TransactionLimitType.ALLOCATION,
            request.allocationSpendControls().getAllocationId().toUuid());
    final TransactionLimit cardLimit =
        allocationLimit.copyForType(
            TransactionLimitType.CARD, request.cardAllocation().getId().toUuid());

    BeanUtils.setNotNull(
        CurrencyLimit.toMap(request.allocationSpendControls().getLimits()), cardLimit::setLimits);
    BeanUtils.setNotNull(
        request.allocationSpendControls().getDisabledMccGroups(), cardLimit::setDisabledMccGroups);
    BeanUtils.setNotNull(
        request.allocationSpendControls().getDisabledPaymentTypes(),
        cardLimit::setDisabledPaymentTypes);
    BeanUtils.setNotNull(
        request.allocationSpendControls().getDisableForeign(), cardLimit::setDisableForeign);

    return transactionLimitRepository.save(cardLimit);
  }

  @Transactional
  TransactionLimit updateAllocationSpendLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Boolean disableForeign) {

    return updateSpendLimit(
        businessId,
        TransactionLimitType.ALLOCATION,
        allocationId.toUuid(),
        transactionLimits,
        disabledMccGroups,
        disabledPaymentTypes,
        disableForeign);
  }

  public record CardSpendControls(
      @NonNull Card card,
      @NonNull CardAllocation cardAllocation,
      @NonNull CardAllocationSpendControls allocationSpendControls)
      implements AllocationRelated {

    @Override
    public TypedId<AllocationId> getAllocationId() {
      return allocationSpendControls.getAllocationId();
    }

    @Override
    public TypedId<BusinessId> getBusinessId() {
      return card.getBusinessId();
    }
  }

  @Transactional
  @PreAuthorize("hasPermission(#request, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public TransactionLimit updateCardSpendLimit(@NonNull final CardSpendControls request) {
    return updateSpendLimit(
        request.card().getBusinessId(),
        TransactionLimitType.CARD,
        request.cardAllocation().getId().toUuid(),
        CurrencyLimit.toMap(request.allocationSpendControls().getLimits()),
        request.allocationSpendControls().getDisabledMccGroups(),
        request.allocationSpendControls().getDisabledPaymentTypes(),
        request.allocationSpendControls().getDisableForeign());
  }

  private TransactionLimit updateSpendLimit(
      TypedId<BusinessId> businessId,
      TransactionLimitType limitType,
      UUID ownerId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Boolean disableForeign) {

    TransactionLimit transactionLimit = retrieveSpendLimit(businessId, limitType, ownerId);

    BeanUtils.setNotNull(transactionLimits, transactionLimit::setLimits);
    BeanUtils.setNotNull(disabledMccGroups, transactionLimit::setDisabledMccGroups);
    BeanUtils.setNotNull(disabledPaymentTypes, transactionLimit::setDisabledPaymentTypes);
    BeanUtils.setNotNull(disableForeign, transactionLimit::setDisableForeign);

    return transactionLimitRepository.save(transactionLimit);
  }

  void ensureWithinLimit(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<CardId> cardId,
      Amount amount,
      Integer mccCode,
      AuthorizationMethod authorizationMethod,
      boolean foreign) {

    final CardAllocation cardAllocation =
        cardAllocationRepository
            .findByCardIdAndAllocationId(cardId, allocationId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.CARD_ALLOCATION, cardId, allocationId));

    MccGroup mccGroup = MccGroup.fromMcc(mccCode);
    PaymentType paymentType = PaymentType.from(authorizationMethod);

    // card mcc groups and payment types
    final TransactionLimit cardTransactionLimits =
        retrieveSpendLimit(businessId, TransactionLimitType.CARD, cardAllocation.getId().toUuid());
    if (cardTransactionLimits.getDisabledMccGroups().contains(mccGroup)) {
      throw new SpendControlViolationException(cardId, TransactionLimitType.CARD, mccGroup);
    }
    if (cardTransactionLimits.getDisabledPaymentTypes().contains(paymentType)) {
      throw new SpendControlViolationException(cardId, TransactionLimitType.CARD, paymentType);
    }

    if (cardTransactionLimits.getDisableForeign() && foreign) {
      throw new SpendControlViolationException(cardId, TransactionLimitType.CARD);
    }

    // spend limits
    final CardAllocationSpendingDaily cardAllocationSpendingDaily =
        accountActivityRepository.findCardAllocationSpendingDaily(
            businessId, allocationId, cardId, 30);

    // check card limits
    withinLimit(
        cardId,
        TransactionLimitType.CARD,
        amount,
        cardAllocationSpendingDaily.getCardSpendings().getOrDefault(amount.getCurrency(), Map.of()),
        cardTransactionLimits);
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
        LocalDate startDate =
            LocalDate.now(ZoneOffset.UTC).minusDays(limit.getKey().getDuration().toDays());

        BigDecimal usage =
            totalSpendings.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(startDate))
                .map(e -> e.getValue().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = limit.getValue().add(amount.getAmount()).add(usage);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
          throw new LimitViolationException(
              limitOwner,
              transactionLimitType,
              LimitType.PURCHASE,
              limit.getKey(),
              amount,
              remaining.abs());
        }
      }
    }
  }
}
