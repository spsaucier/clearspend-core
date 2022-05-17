package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.ledger.JournalEntryId;
import com.clearspend.capital.common.typedid.data.ledger.LedgerAccountId;
import com.clearspend.capital.common.typedid.data.ledger.PostingId;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("JavaTimeDefaultTimeZone")
class BusinessTransactionTransactionLimitServiceTest extends BaseCapitalTest {

  @Autowired private BusinessSettingsService businessSettingsService;

  private final TypedId<AccountId> accountId = new TypedId<>(UUID.randomUUID());
  private final TypedId<BusinessId> businessId = new TypedId<>(UUID.randomUUID());
  private final TypedId<AllocationId> allocationId = new TypedId<>(UUID.randomUUID());
  private final TypedId<JournalEntryId> journalEntryId = new TypedId<>(UUID.randomUUID());
  private final TypedId<LedgerAccountId> ledgerAccountId = new TypedId<>(UUID.randomUUID());
  private final TypedId<PostingId> postingId = new TypedId<>(UUID.randomUUID());

  @Test
  void withinLimit_noAdjustments() {
    HashMap<LimitPeriod, BigDecimal> allocationDurationMap = new HashMap<>();
    allocationDurationMap.put(LimitPeriod.MONTHLY, BigDecimal.TEN);

    // under limit
    businessSettingsService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.valueOf(9.99)),
        Collections.emptyList(),
        allocationDurationMap);

    // exact limit
    businessSettingsService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.TEN),
        Collections.emptyList(),
        allocationDurationMap);

    // over limit
    assertThrows(
        LimitViolationException.class,
        () ->
            businessSettingsService.withinLimit(
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
    businessSettingsService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.valueOf(7.99)),
        List.of(
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE),
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE)),
        allocationDurationMap);

    // exact limit
    businessSettingsService.withinLimit(
        businessId,
        AdjustmentType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.valueOf(8.0)),
        List.of(
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE),
            newAdjustment(AdjustmentType.DEPOSIT, BigDecimal.ONE)),
        allocationDurationMap);

    // over monthly limit
    assertThrows(
        LimitViolationException.class,
        () ->
            businessSettingsService.withinLimit(
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
    businessSettingsService.withinLimit(
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
        allocationId,
        accountId,
        ledgerAccountId,
        journalEntryId,
        postingId,
        deposit,
        time,
        Amount.of(Currency.USD, amount));
  }
}
