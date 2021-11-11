package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import com.tranwall.capital.data.repository.AccountActivityRepository;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import com.tranwall.capital.service.type.PageToken;
import java.math.BigDecimal;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Slf4j
@Transactional
public class AccountActivityServiceTest extends BaseCapitalTest {

  @Autowired TestHelper testHelper;
  @Autowired BusinessBankAccountService businessBankAccountService;
  @Autowired BusinessService businessService;
  @Autowired AccountService accountService;
  @Autowired AccountActivityRepository accountActivityRepository;
  @Autowired AccountActivityService accountActivityService;
  @Autowired AdjustmentService adjustmentService;

  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
  }

  @Test
  void recordAccountActivityOnBusinessBankAccountTransaction() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessAndAllocationsRecord.business().getId());
    AdjustmentRecord adjustmentRecord =
        businessBankAccountService.transactBankAccount(
            businessAndAllocationsRecord.business().getId(),
            businessBankAccountId,
            FundsTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            true);

    int count =
        accountActivityRepository.countByBusinessId(
            businessAndAllocationsRecord.business().getId());
    Assertions.assertEquals(1, count);
  }

  @Test
  void recordAccountActivityOnReallocationBusinessFunds() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();
    final TypedId<AllocationId> rootAllocationId =
        businessAndAllocationsRecord.allocationRecord().allocation().getId();
    accountService.depositFunds(
        business.getId(),
        businessAndAllocationsRecord.allocationRecord().account(),
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account rootAllocationAccount = businessAndAllocationsRecord.allocationRecord().account();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            businessAndAllocationsRecord.allocationRecord().allocation().getId());
    accountService.reallocateFunds(
        rootAllocationAccount.getId(),
        parentAllocationRecord.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));

    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        FundsTransactType.DEPOSIT,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    int count = accountActivityRepository.countByBusinessId(business.getId());
    Assertions.assertEquals(2, count);
  }

  @Test
  void createAccountActivity() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(business.getId());
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            businessAndAllocationsRecord.allocationRecord().allocation().getId(),
            false);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        FundsTransactType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.TEN),
        true);

    int count = accountActivityRepository.countByBusinessId(business.getId());
    Assertions.assertEquals(1, count);
  }

  @Test
  void retrieveLatestAccountActivity() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();
    final TypedId<AllocationId> rootAllocationId =
        businessAndAllocationsRecord.allocationRecord().allocation().getId();
    accountService.depositFunds(
        business.getId(),
        businessAndAllocationsRecord.allocationRecord().account(),
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account rootAllocationAccount = businessAndAllocationsRecord.allocationRecord().account();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            businessAndAllocationsRecord.allocationRecord().allocation().getId());
    accountService.reallocateFunds(
        rootAllocationAccount.getId(),
        parentAllocationRecord.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        FundsTransactType.DEPOSIT,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    Page<AccountActivityResponse> accountActivity =
        accountActivityService.getFilteredAccountActivity(
            business.getId(),
            new AccountActivityFilterCriteria(
                null, null, null, null, null, null, new PageToken(0, 10, null)));

    assertThat(accountActivity).hasSize(2);
  }

  @Test
  void retrieveAllAccountActivityFilterByAllocationType() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessAndAllocationsRecord.business().getId());
    Business business = businessAndAllocationsRecord.business();
    AdjustmentRecord adjustmentRecord =
        businessBankAccountService.transactBankAccount(
            business.getId(),
            businessBankAccountId,
            FundsTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            businessAndAllocationsRecord.allocationRecord().allocation().getId(),
            false);
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            businessAndAllocationsRecord.allocationRecord().allocation().getId());
    accountService.reallocateFunds(
        account.getId(),
        parentAllocationRecord.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        FundsTransactType.WITHDRAW,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    Page<AccountActivityResponse> withdrawalFilteredAccountActivity =
        accountActivityService.getFilteredAccountActivity(
            business.getId(),
            new AccountActivityFilterCriteria(
                null,
                null,
                null,
                AccountActivityType.REALLOCATE,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(withdrawalFilteredAccountActivity).hasSize(2);

    Page<AccountActivityResponse> depositFilteredAccountActivity =
        accountActivityService.getFilteredAccountActivity(
            business.getId(),
            new AccountActivityFilterCriteria(
                null,
                null,
                null,
                AccountActivityType.BANK_DEPOSIT,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(depositFilteredAccountActivity).hasSize(1);
  }
}
