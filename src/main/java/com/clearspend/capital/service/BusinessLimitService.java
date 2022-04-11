package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.business.BusinessLimit.BusinessLimitOperationRecord;
import com.clearspend.capital.controller.type.business.BusinessLimit.BusinessLimitRecord;
import com.clearspend.capital.controller.type.business.BusinessLimit.LimitOperationRecord;
import com.clearspend.capital.controller.type.business.BusinessLimit.LimitPeriodOperationRecord;
import com.clearspend.capital.controller.type.business.BusinessLimit.LimitPeriodRecord;
import com.clearspend.capital.controller.type.business.BusinessLimit.LimitRecord;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.business.BusinessLimit;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.business.BusinessLimitRepository;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BusinessLimitService {

  private static final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> DEFAULT_LIMITS =
      Map.of(
          Currency.USD,
          Map.of(
              LimitType.ACH_DEPOSIT,
              Map.of(
                  LimitPeriod.DAILY,
                  BigDecimal.valueOf(10000),
                  LimitPeriod.MONTHLY,
                  BigDecimal.valueOf(30000)),
              LimitType.ACH_WITHDRAW,
              Map.of(
                  LimitPeriod.DAILY,
                  BigDecimal.valueOf(10000),
                  LimitPeriod.MONTHLY,
                  BigDecimal.valueOf(30000))));

  private static final Map<Currency, Map<LimitType, Map<LimitPeriod, Integer>>>
      DEFAULT_OPERATION_LIMITS =
          Map.of(
              Currency.USD,
              Map.of(
                  LimitType.ACH_DEPOSIT, Map.of(LimitPeriod.DAILY, 2, LimitPeriod.MONTHLY, 6),
                  LimitType.ACH_WITHDRAW, Map.of(LimitPeriod.DAILY, 2, LimitPeriod.MONTHLY, 6)));

  private final BusinessLimitRepository businessLimitRepository;
  private final AdjustmentService adjustmentService;

  private final CardRepository cardRepository;

  private final Integer issuedPhysicalCardDefaultLimit;

  public BusinessLimitService(
      BusinessLimitRepository businessLimitRepository,
      AdjustmentService adjustmentService,
      CardRepository cardRepository,
      @Value("${clearspend.business.limit.issuance.card.physical}")
          Integer issuedPhysicalCardDefaultLimit) {
    this.businessLimitRepository = businessLimitRepository;
    this.adjustmentService = adjustmentService;
    this.cardRepository = cardRepository;
    this.issuedPhysicalCardDefaultLimit = issuedPhysicalCardDefaultLimit;
  }

  public BusinessLimit initializeBusinessLimit(TypedId<BusinessId> businessId) {
    return businessLimitRepository.save(
        new BusinessLimit(
            businessId, DEFAULT_LIMITS, DEFAULT_OPERATION_LIMITS, issuedPhysicalCardDefaultLimit));
  }

  public BusinessLimit retrieveBusinessLimit(TypedId<BusinessId> businessId) {
    BusinessLimit businessLimit = findBusinessLimit(businessId);
    businessLimit.setIssuedPhysicalCardsTotal(
        cardRepository.countByBusinessIdAndType(businessId, CardType.PHYSICAL));

    return businessLimit;
  }

  private BusinessLimit findBusinessLimit(TypedId<BusinessId> businessId) {
    return businessLimitRepository
        .findByBusinessId(businessId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_LIMIT, businessId));
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_FUNDS')")
  public BusinessLimit updateBusinessLimits(
      TypedId<BusinessId> businessId,
      com.clearspend.capital.controller.type.business.BusinessLimit updateBusinessLimit) {
    BusinessLimit businessLimit = findBusinessLimit(businessId);

    if (CollectionUtils.isNotEmpty(updateBusinessLimit.getLimits())) {
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
          businessLimit.getLimits().entrySet().stream().collect(toMapCurrencyLimits());
      updateBusinessLimit.getLimits().forEach(updateBusinessLimitsForCurrency(limits));
      businessLimit.setLimits(limits);
    }

    if (CollectionUtils.isNotEmpty(updateBusinessLimit.getOperationLimits())) {
      Map<Currency, Map<LimitType, Map<LimitPeriod, Integer>>> operationLimits =
          businessLimit.getOperationLimits().entrySet().stream()
              .collect(toMapCurrencyOperationLimits());
      updateBusinessLimit
          .getOperationLimits()
          .forEach(updateBusinessLimitOperationsForCurrency(operationLimits));
      businessLimit.setOperationLimits(operationLimits);
    }

    BeanUtils.setNotNull(
        updateBusinessLimit.getIssuedPhysicalCardsLimit(),
        businessLimit::setIssuedPhysicalCardsLimit);

    businessLimitRepository.save(businessLimit);

    return businessLimit;
  }

  private Collector<
          Entry<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>>,
          ?,
          Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>>>
      toMapCurrencyLimits() {
    return Collectors.toMap(
        Entry::getKey,
        limitType ->
            limitType.getValue().entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        limitPeriod ->
                            limitPeriod.getValue().entrySet().stream()
                                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))));
  }

  private Collector<
          Entry<Currency, Map<LimitType, Map<LimitPeriod, Integer>>>,
          ?,
          Map<Currency, Map<LimitType, Map<LimitPeriod, Integer>>>>
      toMapCurrencyOperationLimits() {
    return Collectors.toMap(
        Entry::getKey,
        operationsLimitType ->
            operationsLimitType.getValue().entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        operationsLimitPeriod ->
                            operationsLimitPeriod.getValue().entrySet().stream()
                                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))));
  }

  private Consumer<LimitRecord> updateBusinessLimitsForCurrency(
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits) {
    return limitRecord -> {
      Map<LimitType, Map<LimitPeriod, BigDecimal>> businessLimitTypeMapMap =
          limits.computeIfAbsent(limitRecord.currency(), k -> new HashMap<>());

      if (limitRecord.businessLimits() == null) {
        limits.remove(limitRecord.currency());
      } else {
        limitRecord
            .businessLimits()
            .forEach(updateBusinessLimitsForLimitType(businessLimitTypeMapMap));
      }
    };
  }

  private Consumer<BusinessLimitRecord> updateBusinessLimitsForLimitType(
      Map<LimitType, Map<LimitPeriod, BigDecimal>> businessLimitTypeMapMap) {
    return businessLimitRecord -> {
      Map<LimitPeriod, BigDecimal> limitPeriodBigDecimalMap =
          businessLimitTypeMapMap.computeIfAbsent(
              businessLimitRecord.businessLimitType(), k -> new HashMap<>());
      if (businessLimitRecord.limitPeriods() == null) {
        businessLimitTypeMapMap.remove(businessLimitRecord.businessLimitType());
      } else {
        businessLimitRecord
            .limitPeriods()
            .forEach(updateBusinessLimitsForPeriod(limitPeriodBigDecimalMap));
      }
    };
  }

  private Consumer<LimitPeriodRecord> updateBusinessLimitsForPeriod(
      Map<LimitPeriod, BigDecimal> limitPeriodBigDecimalMap) {
    return limitPeriodRecord -> {
      BigDecimal bigDecimal = limitPeriodBigDecimalMap.get(limitPeriodRecord.period());
      if (limitPeriodRecord.value() == null) {
        limitPeriodBigDecimalMap.remove(limitPeriodRecord.period());
      } else if (bigDecimal == null || limitPeriodRecord.value().compareTo(bigDecimal) != 0) {
        limitPeriodBigDecimalMap.put(limitPeriodRecord.period(), limitPeriodRecord.value());
      }
    };
  }

  private Consumer<LimitOperationRecord> updateBusinessLimitOperationsForCurrency(
      Map<Currency, Map<LimitType, Map<LimitPeriod, Integer>>> operationLimits) {
    return limitRecord -> {
      Map<LimitType, Map<LimitPeriod, Integer>> businessLimitTypeMapMap =
          operationLimits.computeIfAbsent(limitRecord.currency(), k -> new HashMap<>());

      if (limitRecord.businessLimitOperations() == null) {
        operationLimits.remove(limitRecord.currency());
      } else {
        limitRecord
            .businessLimitOperations()
            .forEach(updateBusinessLimitOperationsForLimitType(businessLimitTypeMapMap));
      }
    };
  }

  private Consumer<BusinessLimitOperationRecord> updateBusinessLimitOperationsForLimitType(
      Map<LimitType, Map<LimitPeriod, Integer>> businessLimitTypeMapMap) {
    return businessLimitRecord -> {
      Map<LimitPeriod, Integer> limitPeriodBigDecimalMap =
          businessLimitTypeMapMap.computeIfAbsent(
              businessLimitRecord.businessLimitType(), k -> new HashMap<>());
      if (businessLimitRecord.limitOperationPeriods() == null) {
        businessLimitTypeMapMap.remove(businessLimitRecord.businessLimitType());
      } else {
        businessLimitRecord
            .limitOperationPeriods()
            .forEach(
                updateBusinessLimitOperationsForPeriodUnderBusinessType(limitPeriodBigDecimalMap));
      }
    };
  }

  private Consumer<LimitPeriodOperationRecord>
      updateBusinessLimitOperationsForPeriodUnderBusinessType(
          Map<LimitPeriod, Integer> limitPeriodBigDecimalMap) {
    return limitPeriodRecord -> {
      Integer integer = limitPeriodBigDecimalMap.get(limitPeriodRecord.period());
      if (limitPeriodRecord.value() == null) {
        limitPeriodBigDecimalMap.remove(limitPeriodRecord.period());
      } else if (integer == null || limitPeriodRecord.value().compareTo(integer) != 0) {
        limitPeriodBigDecimalMap.put(limitPeriodRecord.period(), limitPeriodRecord.value());
      }
    };
  }

  protected void ensureWithinDepositLimit(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensureNonNegative();
    List<Adjustment> adjustments =
        adjustmentService.retrieveBusinessAdjustments(
            businessId, List.of(AdjustmentType.DEPOSIT), 30);
    BusinessLimit limit = findBusinessLimit(businessId);

    // evaluate limits
    withinOperationLimits(
        businessId,
        LimitType.ACH_DEPOSIT,
        adjustments,
        limit
            .getOperationLimits()
            .getOrDefault(amount.getCurrency(), Map.of())
            .get(LimitType.ACH_DEPOSIT));
    withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        amount,
        adjustments,
        limit.getLimits().get(amount.getCurrency()).get(LimitType.ACH_DEPOSIT));
  }

  public void ensureWithinWithdrawLimit(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensureNonNegative();
    List<Adjustment> adjustments =
        adjustmentService.retrieveBusinessAdjustments(
            businessId, List.of(AdjustmentType.WITHDRAW), 30);
    BusinessLimit limit = findBusinessLimit(businessId);

    // evaluate limits
    withinOperationLimits(
        businessId,
        LimitType.ACH_WITHDRAW,
        adjustments,
        limit
            .getOperationLimits()
            .getOrDefault(amount.getCurrency(), Map.of())
            .get(LimitType.ACH_WITHDRAW));
    withinLimit(
        businessId,
        AdjustmentType.WITHDRAW,
        amount,
        adjustments,
        limit.getLimits().get(amount.getCurrency()).get(LimitType.ACH_WITHDRAW));
  }

  @VisibleForTesting
  void withinLimit(
      TypedId<BusinessId> businessId,
      AdjustmentType type,
      Amount amount,
      List<Adjustment> adjustments,
      Map<LimitPeriod, BigDecimal> limits) {
    if (limits != null) {
      for (Entry<LimitPeriod, BigDecimal> limit : limits.entrySet()) {
        OffsetDateTime startDate =
            OffsetDateTime.now(ZoneOffset.UTC).minus(limit.getKey().getDuration());

        BigDecimal usage =
            adjustments.stream()
                .filter(adjustment -> adjustment.getEffectiveDate().isAfter(startDate))
                .map(
                    adjustment ->
                        amount.isPositive()
                            ? adjustment.getAmount().getAmount()
                            : adjustment.getAmount().getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = usage.add(amount.getAmount());
        if (total.compareTo(limit.getValue()) > 0) {
          throw new LimitViolationException(
              businessId,
              TransactionLimitType.BUSINESS,
              type == AdjustmentType.DEPOSIT ? LimitType.ACH_DEPOSIT : LimitType.ACH_WITHDRAW,
              limit.getKey(),
              amount,
              total.subtract(limit.getValue()));
        }
      }
    }
  }

  void withinOperationLimits(
      TypedId<BusinessId> businessId,
      LimitType limitType,
      List<Adjustment> adjustments,
      Map<LimitPeriod, Integer> limits) {
    if (limits != null) {
      for (Entry<LimitPeriod, Integer> limit : limits.entrySet()) {
        long operationAmount =
            adjustments.stream()
                .filter(
                    adjustment ->
                        adjustment
                            .getEffectiveDate()
                            .isAfter(
                                OffsetDateTime.now(ZoneOffset.UTC)
                                    .minus(limit.getKey().getDuration())))
                .count();
        if (operationAmount >= limit.getValue()) {
          throw new InvalidRequestException(
              "Business %s exceeded allowed operation amount of limitType %s for period %s"
                  .formatted(businessId, limitType, limit.getKey()));
        }
      }
    }
  }
}
