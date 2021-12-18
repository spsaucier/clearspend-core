package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.BusinessLimit;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.repository.BusinessLimitRepository;
import com.google.common.annotations.VisibleForTesting;
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

    limits.put(Currency.USD, new HashMap<>());

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
        limit.getLimits().get(amount.getCurrency()).get(LimitType.ACH_DEPOSIT));
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
          throw new InsufficientFundsException("Business", businessId, type, amount);
        }
      }
    }
  }
}
