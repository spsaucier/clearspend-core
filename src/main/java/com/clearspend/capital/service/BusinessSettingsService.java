package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.OperationLimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.business.BusinessSettings.BusinessLimitOperationRecord;
import com.clearspend.capital.controller.type.business.BusinessSettings.BusinessLimitRecord;
import com.clearspend.capital.controller.type.business.BusinessSettings.LimitOperationRecord;
import com.clearspend.capital.controller.type.business.BusinessSettings.LimitPeriodOperationRecord;
import com.clearspend.capital.controller.type.business.BusinessSettings.LimitPeriodRecord;
import com.clearspend.capital.controller.type.business.BusinessSettings.LimitRecord;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.business.BusinessSettings;
import com.clearspend.capital.data.model.enums.AchFundsAvailabilityMode;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.business.BusinessSettingsRepository;
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
public class BusinessSettingsService {

  private static final BigDecimal DEFAULT_FOREIGN_TRANSACTION_FEE = new BigDecimal(3);
  private static final AchFundsAvailabilityMode DEFAULT_ACH_FUNDS_AVAILABILITY_MODE =
      AchFundsAvailabilityMode.STANDARD;
  private static final BigDecimal DEFAULT_IMMEDIATE_ACH_FUNDS_LIMIT = BigDecimal.ZERO;

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

  private final BusinessSettingsRepository businessSettingsRepository;
  private final AdjustmentService adjustmentService;

  private final CardRepository cardRepository;

  private final Integer issuedPhysicalCardDefaultLimit;

  public BusinessSettingsService(
      BusinessSettingsRepository businessSettingsRepository,
      AdjustmentService adjustmentService,
      CardRepository cardRepository,
      @Value("${clearspend.business.limit.issuance.card.physical}")
          Integer issuedPhysicalCardDefaultLimit) {
    this.businessSettingsRepository = businessSettingsRepository;
    this.adjustmentService = adjustmentService;
    this.cardRepository = cardRepository;
    this.issuedPhysicalCardDefaultLimit = issuedPhysicalCardDefaultLimit;
  }

  BusinessSettings initializeBusinessSettings(TypedId<BusinessId> businessId) {
    return businessSettingsRepository.save(
        new BusinessSettings(
            businessId,
            DEFAULT_LIMITS,
            DEFAULT_OPERATION_LIMITS,
            issuedPhysicalCardDefaultLimit,
            DEFAULT_FOREIGN_TRANSACTION_FEE,
            DEFAULT_ACH_FUNDS_AVAILABILITY_MODE,
            DEFAULT_IMMEDIATE_ACH_FUNDS_LIMIT));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'READ')")
  public BusinessSettings retrieveBusinessSettings(TypedId<BusinessId> businessId) {
    return retrieveBusinessSettingsForService(businessId);
  }

  BusinessSettings retrieveBusinessSettingsForService(final TypedId<BusinessId> businessId) {
    BusinessSettings businessSettings = findBusinessSettings(businessId);
    businessSettings.setIssuedPhysicalCardsTotal(
        cardRepository.countByBusinessIdAndType(businessId, CardType.PHYSICAL));

    return businessSettings;
  }

  private BusinessSettings findBusinessSettings(TypedId<BusinessId> businessId) {
    return businessSettingsRepository
        .findByBusinessId(businessId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_LIMIT, businessId));
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_FUNDS')")
  public BusinessSettings updateBusinessSettings(
      TypedId<BusinessId> businessId,
      com.clearspend.capital.controller.type.business.BusinessSettings updateBusinessSettings) {
    BusinessSettings businessSettings = findBusinessSettings(businessId);

    if (CollectionUtils.isNotEmpty(updateBusinessSettings.getLimits())) {
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
          businessSettings.getLimits().entrySet().stream().collect(toMapCurrencyLimits());
      updateBusinessSettings.getLimits().forEach(updateBusinessLimitsForCurrency(limits));
      businessSettings.setLimits(limits);
    }

    if (CollectionUtils.isNotEmpty(updateBusinessSettings.getOperationLimits())) {
      Map<Currency, Map<LimitType, Map<LimitPeriod, Integer>>> operationLimits =
          businessSettings.getOperationLimits().entrySet().stream()
              .collect(toMapCurrencyOperationLimits());
      updateBusinessSettings
          .getOperationLimits()
          .forEach(updateBusinessLimitOperationsForCurrency(operationLimits));
      businessSettings.setOperationLimits(operationLimits);
    }

    BeanUtils.setNotNull(
        updateBusinessSettings.getIssuedPhysicalCardsLimit(),
        businessSettings::setIssuedPhysicalCardsLimit);
    BeanUtils.setNotNull(
        updateBusinessSettings.getForeignTransactionFeePercents(),
        businessSettings::setForeignTransactionFeePercents);
    BeanUtils.setNotNull(
        updateBusinessSettings.getAchFundsAvailabilityMode(),
        businessSettings::setAchFundsAvailabilityMode);
    BeanUtils.setNotNull(
        updateBusinessSettings.getImmediateAchFundsLimit(),
        businessSettings::setImmediateAchFundsLimit);

    businessSettingsRepository.save(businessSettings);

    return businessSettings;
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

  void ensureWithinDepositLimit(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensureNonNegative();
    List<Adjustment> adjustments =
        adjustmentService.retrieveBusinessAdjustments(
            businessId, List.of(AdjustmentType.DEPOSIT), 30);
    BusinessSettings limit = findBusinessSettings(businessId);

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

  void ensureWithinWithdrawLimit(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensureNonNegative();
    List<Adjustment> adjustments =
        adjustmentService.retrieveBusinessAdjustments(
            businessId, List.of(AdjustmentType.WITHDRAW), 30);
    BusinessSettings limit = findBusinessSettings(businessId);

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

  private void withinOperationLimits(
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
          throw new OperationLimitViolationException(
              businessId,
              TransactionLimitType.BUSINESS,
              limitType,
              limit.getKey(),
              limit.getValue());
        }
      }
    }
  }
}
