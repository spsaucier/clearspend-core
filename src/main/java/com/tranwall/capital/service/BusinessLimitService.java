package com.tranwall.capital.service;

import com.google.common.annotations.VisibleForTesting;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.utils.BigDecimalUtils;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.BusinessLimit;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.BusinessLimitRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
    return businessLimitRepository.save(
        new BusinessLimit(
            businessId,
            Amount.of(Currency.USD, BigDecimal.valueOf(10_000)),
            Amount.of(Currency.USD, BigDecimal.valueOf(300_000)),
            Amount.of(Currency.USD, BigDecimal.valueOf(10_000)),
            Amount.of(Currency.USD, BigDecimal.valueOf(300_000))));
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
        businessId, AdjustmentType.DEPOSIT, amount, adjustments, limit.getDailyDepositLimit(), 1);
    withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        amount,
        adjustments,
        limit.getMonthlyDepositLimit(),
        30);
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
        businessId, AdjustmentType.WITHDRAW, amount, adjustments, limit.getDailyWithdrawLimit(), 1);
    withinLimit(
        businessId,
        AdjustmentType.WITHDRAW,
        amount,
        adjustments,
        limit.getMonthlyWithdrawLimit(),
        30);
  }

  @VisibleForTesting
  void withinLimit(
      TypedId<BusinessId> businessId,
      AdjustmentType type,
      Amount amount,
      List<Adjustment> adjustments,
      Amount limitAmount,
      int days) {
    OffsetDateTime startDate = OffsetDateTime.now().minusDays(days);

    BigDecimal usage =
        adjustments.stream()
            .filter(adjustment -> adjustment.getEffectiveDate().isAfter(startDate))
            .map(
                adjustment ->
                    amount.isPositive()
                        ? adjustment.getAmount().getAmount()
                        : adjustment.getAmount().getAmount().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (BigDecimalUtils.isLargerThan(usage.add(amount.getAmount()), limitAmount.getAmount())) {
      throw new InsufficientFundsException("Business", businessId, type, amount);
    }
  }
}
