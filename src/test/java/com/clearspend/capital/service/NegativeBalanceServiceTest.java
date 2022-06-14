package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.AdjustmentService.AdjustmentPersistedEvent;
import com.clearspend.capital.service.AdjustmentService.AdjustmentRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import java.math.BigDecimal;
import java.util.UUID;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NegativeBalanceServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private NegativeBalanceService negativeBalanceService;
  @Autowired private AdjustmentService adjustmentService;
  @Autowired private StorageProvider jobStorageProvider;
  private Business business;
  private User user;
  private AllocationRecord rootAllocation;
  private AllocationRecord negativeAllocation;

  @BeforeEach
  void init() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(100L);
    business = createBusinessRecord.business();
    business.setStatus(BusinessStatus.ACTIVE);

    rootAllocation = createBusinessRecord.allocationRecord();

    user = createBusinessRecord.user();
    testHelper.setCurrentUser(user);

    negativeAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Negative Allocation",
            rootAllocation.allocation().getId());
  }

  @Test
  void test_businessExpenditureSuspensionIfTotalBalanceIsNegative() {
    // given
    AdjustmentRecord adjustmentRecord =
        adjustmentService.recordManualAdjustment(
            negativeAllocation.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-110L)));

    // when (since @TransactionalEventListener doesn't work with our integration tests - trigger it
    // manually)
    negativeBalanceService.onAdjustmentCreatedOrUpdated(
        new AdjustmentPersistedEvent(adjustmentRecord.adjustment()));

    // then
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.SUSPENDED_EXPENDITURE);
    assertThat(scheduledJobExists(business.getId().toUuid())).isFalse();
  }

  @Test
  void test_correctionProcedureIsScheduledIfTotalBalanceIsPositive() {
    // given
    AdjustmentRecord adjustmentRecord =
        adjustmentService.recordManualAdjustment(
            negativeAllocation.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-10L)));

    // when
    negativeBalanceService.onAdjustmentCreatedOrUpdated(
        new AdjustmentPersistedEvent(adjustmentRecord.adjustment()));

    // then
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.ACTIVE);
    assertThat(scheduledJobExists(business.getId().toUuid())).isTrue();
  }

  @Test
  void test_NegativeBalanceCorrectionSingleNegativeAccount() {
    // given
    AdjustmentRecord adjustmentRecord =
        adjustmentService.recordManualAdjustment(
            negativeAllocation.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-10L)));

    // when
    testHelper.runWithJobUser(
        user, () -> negativeBalanceService.correctNegativeBalances(business.getId()));

    // then
    assertThat(rootAllocation.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.valueOf(90L));
    assertThat(negativeAllocation.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void test_NegativeBalanceCorrectionMultipleNegativeAccount() {
    // given
    adjustmentService.recordManualAdjustment(
        negativeAllocation.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-10L)));

    AllocationRecord negativeAllocation1 =
        testHelper.createAllocation(
            business.getId(), "Negative Allocation1", rootAllocation.allocation().getId());
    adjustmentService.recordManualAdjustment(
        negativeAllocation1.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-15L)));

    AllocationRecord negativeAllocation2 =
        testHelper.createAllocation(
            business.getId(), "Negative Allocation2", rootAllocation.allocation().getId());
    adjustmentService.recordManualAdjustment(
        negativeAllocation2.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-20L)));

    // when
    testHelper.runWithJobUser(
        user, () -> negativeBalanceService.correctNegativeBalances(business.getId()));

    // then
    assertThat(rootAllocation.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.valueOf(55L));

    assertThat(negativeAllocation.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(negativeAllocation1.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(negativeAllocation2.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void test_NegativeBalanceCorrectionMultipleNegativeAccountWithMultiplePositive() {
    // given
    adjustmentService.recordManualAdjustment(
        negativeAllocation.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-20L)));

    AllocationRecord negativeAllocation1 =
        testHelper.createAllocation(
            business.getId(), "Negative Allocation1", rootAllocation.allocation().getId());
    adjustmentService.recordManualAdjustment(
        negativeAllocation1.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-20L)));

    AllocationRecord negativeAllocation2 =
        testHelper.createAllocation(
            business.getId(), "Negative Allocation2", rootAllocation.allocation().getId());
    adjustmentService.recordManualAdjustment(
        negativeAllocation2.account(), Amount.of(Currency.USD, BigDecimal.valueOf(-20L)));

    AllocationRecord positiveAllocation =
        testHelper.createAllocation(
            business.getId(),
            "Positive Allocation",
            rootAllocation.allocation().getId(),
            BigDecimal.valueOf(45L));

    // when
    testHelper.runWithJobUser(
        user, () -> negativeBalanceService.correctNegativeBalances(business.getId()));

    // then
    assertThat(rootAllocation.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(positiveAllocation.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.valueOf(40L));

    assertThat(negativeAllocation.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(negativeAllocation1.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(negativeAllocation2.account().getLedgerBalance().getAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  private boolean scheduledJobExists(UUID jobId) {
    try {
      jobStorageProvider.getJobById(jobId);
      return true;
    } catch (JobNotFoundException e) {
      return false;
    }
  }
}
