package com.tranwall.capital.service;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.activity.AccountActivityRequest;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.activity.PageRequestDTO;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Adjustment;
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
import java.math.BigDecimal;
import java.util.List;
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
        testHelper.createBusinessBankAccount(
            businessAndAllocationsRecord.businessAccount().getBusinessId());
    AdjustmentRecord adjustmentRecord =
        businessBankAccountService.transactBankAccount(
            businessAndAllocationsRecord.business().getId(),
            businessBankAccountId,
            FundsTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            true);

    List<AccountActivity> all = accountActivityRepository.findAll();

    Assertions.assertEquals(1, all.size());
  }

  @Test
  void recordAccountActivityOnReallocationBusinessFunds() {
    Business business = testHelper.createBusiness(program).business();
    accountService.depositFunds(
        business.getId(),
        com.tranwall.capital.common.data.model.Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveBusinessAccount(business.getId(), business.getCurrency(), false);
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), null);
    accountService.reallocateFunds(
        account.getId(),
        parentAllocationRecord.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));

    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        FundsTransactType.DEPOSIT,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    List<AccountActivity> all = accountActivityRepository.findAll();
    Assertions.assertEquals(2, all.size());
  }

  @Test
  void createAccountActivity() {
    Business business = testHelper.createBusiness(program).business();
    Account account =
        accountService.retrieveBusinessAccount(business.getId(), business.getCurrency(), false);
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), null);

    Adjustment adjustment =
        adjustmentService.recordDepositFunds(account, new Amount(Currency.USD, BigDecimal.TEN));
    accountActivityService.recordAccountActivity(AccountActivityType.BANK_DEPOSIT, adjustment);

    List<AccountActivity> all = accountActivityRepository.findAll();
    Assertions.assertEquals(1, all.size());
  }

  @Test
  void retrieveLatestAccountActivity() {
    Business business = testHelper.createBusiness(program).business();
    accountService.depositFunds(
        business.getId(),
        com.tranwall.capital.common.data.model.Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveBusinessAccount(business.getId(), business.getCurrency(), false);
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), null);
    accountService.reallocateFunds(
        account.getId(),
        parentAllocationRecord.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        FundsTransactType.DEPOSIT,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequestDTO(
        PageRequestDTO.builder().pageNumber(0).pageSize(10).orderable(null).build());
    Page<AccountActivityResponse> latestAccountActivity =
        accountActivityService.getFilteredAccountActivity(business.getId(), accountActivityRequest);

    Assertions.assertEquals(2, latestAccountActivity.getTotalElements());
  }

  @Test
  void retrieveAllAccountActivityFilterByAllocationType() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(
            businessAndAllocationsRecord.businessAccount().getBusinessId());
    Business business = businessAndAllocationsRecord.business();
    AdjustmentRecord adjustmentRecord =
        businessBankAccountService.transactBankAccount(
            business.getId(),
            businessBankAccountId,
            FundsTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            false);
    Account account =
        accountService.retrieveBusinessAccount(business.getId(), business.getCurrency(), false);
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), null);
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

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequestDTO(
        PageRequestDTO.builder().pageNumber(0).pageSize(10).build());
    accountActivityRequest.setType(AccountActivityType.BANK_WITHDRAWAL);
    Page<AccountActivityResponse> withdrawalFilteredAccountActivity =
        accountActivityService.getFilteredAccountActivity(business.getId(), accountActivityRequest);

    Assertions.assertEquals(2, withdrawalFilteredAccountActivity.getTotalElements());

    accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequestDTO(
        PageRequestDTO.builder().pageNumber(0).pageSize(10).build());
    accountActivityRequest.setType(AccountActivityType.BANK_DEPOSIT);
    Page<AccountActivityResponse> depositFilteredAccountActivity =
        accountActivityService.getFilteredAccountActivity(business.getId(), accountActivityRequest);

    Assertions.assertEquals(1, depositFilteredAccountActivity.getTotalElements());
  }
}
