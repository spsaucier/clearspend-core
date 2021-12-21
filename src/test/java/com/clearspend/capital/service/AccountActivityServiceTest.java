package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessReallocationType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.NetworkMessageType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.type.PageToken;
import com.fasterxml.jackson.core.JsonProcessingException;
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
  @Autowired NetworkMessageService networkMessageService;

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
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    businessBankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        true);

    int count =
        accountActivityRepository.countByBusinessId(createBusinessRecord.business().getId());
    Assertions.assertEquals(2, count);
  }

  @Test
  void recordAccountActivityOnReallocationBusinessFunds() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    accountService.depositFunds(
        business.getId(),
        createBusinessRecord.allocationRecord().account(),
        createBusinessRecord.allocationRecord().allocation(),
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account rootAllocationAccount = createBusinessRecord.allocationRecord().account();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());
    accountService.reallocateFunds(
        rootAllocationAccount.getId(),
        parentAllocationRecord.account().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(300)));

    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        BusinessReallocationType.ALLOCATION_TO_BUSINESS,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    int count = accountActivityRepository.countByBusinessId(business.getId());
    Assertions.assertEquals(2, count);
  }

  @Test
  void createAccountActivity() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(business.getId());
    accountService.retrieveRootAllocationAccount(
        business.getId(),
        business.getCurrency(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        false);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, BigDecimal.TEN),
        true);

    int count = accountActivityRepository.countByBusinessId(business.getId());
    Assertions.assertEquals(2, count);
  }

  @Test
  void retrieveLatestAccountActivity() throws JsonProcessingException {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    accountService.depositFunds(
        business.getId(),
        createBusinessRecord.allocationRecord().account(),
        createBusinessRecord.allocationRecord().allocation(),
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account rootAllocationAccount = createBusinessRecord.allocationRecord().account();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());
    accountService.reallocateFunds(
        rootAllocationAccount.getId(),
        parentAllocationRecord.account().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        BusinessReallocationType.ALLOCATION_TO_BUSINESS,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    UserService.CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            program,
            Currency.USD);

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(100));

    networkMessageService.processNetworkMessage(
        TestDataController.generateNetworkCommon(
            NetworkMessageType.PRE_AUTH,
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            program,
            amount));

    Page<AccountActivity> accountActivity =
        accountActivityRepository.find(
            business.getId(),
            new AccountActivityFilterCriteria(
                null, null, null, null, null, null, null, new PageToken(0, 10, null)));

    assertThat(accountActivity).hasSize(3);
  }

  @Test
  void retrieveAllAccountActivityFilterByAllocationType() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());
    accountService.reallocateFunds(
        account.getId(),
        parentAllocationRecord.account().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        parentAllocationRecord.allocation().getId(),
        parentAllocationRecord.account().getId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    Page<AccountActivity> withdrawalFilteredAccountActivity =
        accountActivityRepository.find(
            business.getId(),
            new AccountActivityFilterCriteria(
                null,
                null,
                null,
                AccountActivityType.REALLOCATE,
                null,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(withdrawalFilteredAccountActivity).hasSize(2);

    Page<AccountActivity> depositFilteredAccountActivity =
        accountActivityRepository.find(
            business.getId(),
            new AccountActivityFilterCriteria(
                null,
                null,
                null,
                AccountActivityType.BANK_DEPOSIT,
                null,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(depositFilteredAccountActivity).hasSize(1);
  }
}
