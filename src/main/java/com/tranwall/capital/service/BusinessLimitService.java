package com.tranwall.capital.service;

import com.google.common.annotations.VisibleForTesting;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.BusinessLimit;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LimitPeriod;
import com.tranwall.capital.data.model.enums.LimitType;
import com.tranwall.capital.data.repository.BusinessLimitRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessLimitService {

  private final BusinessLimitRepository businessLimitRepository;

  private final AdjustmentService adjustmentService;

  public BusinessLimit initializeBusinessSpendLimit(TypedId<BusinessId> businessId) {
    Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits = new HashMap<>();

    HashMap<LimitPeriod, BigDecimal> allocationDurationMap = new HashMap<>();
    allocationDurationMap.put(LimitPeriod.DAILY, BigDecimal.valueOf(10_000));
    allocationDurationMap.put(LimitPeriod.MONTHLY, BigDecimal.valueOf(30_0000));

    Map<LimitType, Map<LimitPeriod, BigDecimal>> limitTypeMap = new HashMap<>();
    limitTypeMap.put(LimitType.DEPOSIT, allocationDurationMap);
    limitTypeMap.put(LimitType.WITHDRAW, allocationDurationMap);

    limits.put(Currency.USD, limitTypeMap);

    return businessLimitRepository.save(new BusinessLimit(businessId, limits));
  }

  public void ensureWithinDepositLimit(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensurePositive();
    List<Adjustment> adjustments =
        adjustmentService.retrieveBusinessAdjustments(
            businessId, List.of(AdjustmentType.DEPOSIT), 30);
    BusinessLimit limit =
        businessLimitRepository
            .findByBusinessId(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    // evaluate limits
    withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        amount,
        adjustments,
        limit.getLimits().get(amount.getCurrency()).get(LimitType.DEPOSIT));
  }

  public void ensureWithinWithdrawLimit(TypedId<BusinessId> businessId, Amount amount) {
    amount.ensurePositive();
    List<Adjustment> adjustments =
        adjustmentService.retrieveBusinessAdjustments(
            businessId, List.of(AdjustmentType.WITHDRAW), 30);
    BusinessLimit limit =
        businessLimitRepository
            .findByBusinessId(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    // evaluate limits
    withinLimit(
        businessId,
        AdjustmentType.WITHDRAW,
        amount,
        adjustments,
        limit.getLimits().get(amount.getCurrency()).get(LimitType.WITHDRAW));
  }

  @VisibleForTesting
  void withinLimit(
      TypedId<BusinessId> businessId,
      AdjustmentType type,
      Amount amount,
      List<Adjustment> adjustments,
      Map<LimitPeriod, BigDecimal> limits) {
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
        throw new InsufficientFundsException("Business", businessId, type, amount);
      }
    }
  }
}
