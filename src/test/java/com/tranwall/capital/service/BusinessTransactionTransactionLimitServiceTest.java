package com.tranwall.capital.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.JournalEntryId;
import com.tranwall.capital.common.typedid.data.LedgerAccountId;
import com.tranwall.capital.common.typedid.data.PostingId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LimitPeriod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BusinessTransactionTransactionLimitServiceTest extends BaseCapitalTest {

  @Autowired private BusinessLimitService businessLimitService;

  private final TypedId<AccountId> accountId = new TypedId<>(UUID.randomUUID());
  private final TypedId<BusinessId> businessId = new TypedId<>(UUID.randomUUID());
  private final TypedId<JournalEntryId> journalEntryId = new TypedId<>(UUID.randomUUID());
  private final TypedId<LedgerAccountId> ledgerAccountId = new TypedId<>(UUID.randomUUID());
  private final TypedId<PostingId> postingId = new TypedId<>(UUID.randomUUID());

  @Test
  void withinLimit_noAdjustments() {
    HashMap<LimitPeriod, BigDecimal> allocationDurationMap = new HashMap<>();
    allocationDurationMap.put(LimitPeriod.MONTHLY, BigDecimal.TEN);

    // under limit
    businessLimitService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.valueOf(9.99)),
        Collections.emptyList(),
        allocationDurationMap);

    // exact limit
    businessLimitService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.TEN),
        Collections.emptyList(),
        allocationDurationMap);

    // over limit
    assertThrows(
        InsufficientFundsException.class,
        () ->
            businessLimitService.withinLimit(
                businessId,
                AdjustmentType.DEPOSIT,
                Amount.of(Currency.USD, BigDecimal.valueOf(10.01)),
                Collections.emptyList(),
                allocationDurationMap));
  }

  @Test
  void withinLimit_existingAdjustments() {
    HashMap<LimitPeriod, BigDecimal> allocationDurationMap = new HashMap<>();
    allocationDurationMap.put(LimitPeriod.MONTHLY, BigDecimal.TEN);

    // under limit
    businessLimitService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.valueOf(7.99)),
        List.of(
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE),
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE)),
        allocationDurationMap);

    // exact limit
    businessLimitService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.valueOf(8.0)),
        List.of(
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE),
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE)),
        allocationDurationMap);

    // over monthly limit
    assertThrows(
        InsufficientFundsException.class,
        () ->
            businessLimitService.withinLimit(
                businessId,
                AdjustmentType.DEPOSIT,
                Amount.of(Currency.USD, BigDecimal.valueOf(8.01)),
                List.of(
                    newAdjustment(
                        AdjustmentType.DEPOSIT, OffsetDateTime.now().minusDays(20), BigDecimal.ONE),
                    newAdjustment(
                        AdjustmentType.DEPOSIT, OffsetDateTime.now().minusDays(6), BigDecimal.ONE)),
                allocationDurationMap));

    allocationDurationMap.clear();
    allocationDurationMap.put(LimitPeriod.DAILY, BigDecimal.TEN);
    // under daily limit
    businessLimitService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.valueOf(8.01)),
        List.of(
            newAdjustment(
                AdjustmentType.DEPOSIT, OffsetDateTime.now().minusDays(20), BigDecimal.ONE),
            newAdjustment(
                AdjustmentType.DEPOSIT, OffsetDateTime.now().minusHours(6), BigDecimal.ONE)),
        allocationDurationMap);
  }

  private Adjustment newAdjustment(AdjustmentType deposit, BigDecimal amount) {
    return newAdjustment(deposit, OffsetDateTime.now(), amount);
  }

  private Adjustment newAdjustment(AdjustmentType deposit, OffsetDateTime time, BigDecimal amount) {
    return new Adjustment(
        businessId,
        accountId,
        ledgerAccountId,
        journalEntryId,
        postingId,
        deposit,
        time,
        Amount.of(Currency.USD, amount));
  }
}
