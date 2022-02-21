package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.enums.*;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.type.PageToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Slf4j
@Transactional
public class AccountActivityServiceTest extends BaseCapitalTest {

  @Autowired AccountService accountService;
  @Autowired AccountActivityService accountActivityService;
  @Autowired BusinessBankAccountService businessBankAccountService;
  @Autowired BusinessService businessService;
  @Autowired NetworkMessageService networkMessageService;

  @Autowired TestHelper testHelper;
  @Autowired AccountActivityRepository accountActivityRepository;
  @Autowired ExpenseCategoryService expenseCategoryService;

  @Test
  void recordAccountActivityOnBusinessBankAccountTransaction() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    businessBankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccount.getId(),
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
    testHelper.setCurrentUser(createBusinessRecord.user());
    accountService.depositFunds(
        business.getId(),
        createBusinessRecord.allocationRecord().account(),
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
        createBusinessRecord.allocationRecord().allocation().getId(),
        parentAllocationRecord.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    int count = accountActivityRepository.countByBusinessId(business.getId());
    Assertions.assertEquals(2, count);
  }

  @Test
  void createAccountActivity() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(business.getId());
    accountService.retrieveRootAllocationAccount(
        business.getId(),
        business.getCurrency(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        false);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
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
    testHelper.setCurrentUser(createBusinessRecord.user());

    accountService.depositFunds(
        business.getId(),
        createBusinessRecord.allocationRecord().account(),
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
        createBusinessRecord.allocationRecord().allocation().getId(),
        parentAllocationRecord.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    UserService.CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(100));

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user.user(), card, createBusinessRecord.allocationRecord().account(), amount);
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

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
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
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
        createBusinessRecord.allocationRecord().allocation().getId(),
        parentAllocationRecord.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    Page<AccountActivity> withdrawalFilteredAccountActivity =
        accountActivityRepository.find(
            business.getId(),
            new AccountActivityFilterCriteria(
                null,
                null,
                null,
                List.of(AccountActivityType.REALLOCATE),
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
                List.of(AccountActivityType.BANK_DEPOSIT),
                null,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(depositFilteredAccountActivity).hasSize(1);
  }

  @Test
  void getUserAccountActivity() {
    assertThrows(
        RecordNotFoundException.class,
        () -> accountActivityService.findByReceiptId(new TypedId<>(), new TypedId<>()));
  }

  @Test
  void updateAccountActivity_success() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(businessRecord.user());
    AccountActivity accountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().allocation().getId(),
            businessRecord.allocationRecord().allocation().getName(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    log.info("AccountActivity: {}", accountActivity);
    accountActivity.setNotes("");
    ExpenseCategory expenseCategory;
    Integer iconRef = 0;
    if (null != iconRef && iconRef != 0) {
      expenseCategory = expenseCategoryService.retrieveExpenseCategory(iconRef);
      accountActivity.setExpenseDetails(
          new ExpenseDetails(expenseCategory.getIconRef(), expenseCategory.getCategoryName()));
    }
    accountActivityRepository.save(accountActivity);
    AccountActivity accountActivityResult =
        accountActivityRepository.findById(accountActivity.getId()).orElseThrow();
    log.info(String.valueOf(accountActivityResult));
  }
}
