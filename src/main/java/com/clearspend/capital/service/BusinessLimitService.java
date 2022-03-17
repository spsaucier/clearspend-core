package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    BusinessLimit businessLimit =
        businessLimitRepository
            .findByBusinessId(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_LIMIT, businessId));
    businessLimit.setIssuedPhysicalCardsTotal(
        cardRepository.countByBusinessIdAndType(businessId, CardType.PHYSICAL));

    return businessLimit;
  }

  public void ensureWithinDepositLimit(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensureNonNegative();
    List<Adjustment> adjustments =
        adjustmentService.retrieveBusinessAdjustments(
            businessId, List.of(AdjustmentType.DEPOSIT), 30);
    BusinessLimit limit =
        businessLimitRepository
            .findByBusinessId(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

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
    BusinessLimit limit =
        businessLimitRepository
            .findByBusinessId(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

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
        OffsetDateTime startDate = OffsetDateTime.now().minus(limit.getKey().getDuration());

        BigDecimal usage =
            adjustments.stream()
                .filter(adjustment -> adjustment.getEffectiveDate().isAfter(startDate))
                .map(
                    adjustment ->
                        amount.isPositive()
                            ? adjustment.getAmount().getAmount()
                            : adjustment.getAmount().getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (usage.add(amount.getAmount()).compareTo(limit.getValue()) > 0) {
          throw new LimitViolationException(
              businessId,
              TransactionLimitType.BUSINESS,
              type == AdjustmentType.DEPOSIT ? LimitType.ACH_DEPOSIT : LimitType.ACH_WITHDRAW,
              limit.getKey(),
              amount);
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
                            .isAfter(OffsetDateTime.now().minus(limit.getKey().getDuration())))
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
