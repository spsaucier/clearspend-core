package com.clearspend.capital.service;

import static com.clearspend.capital.testutils.data.TestDataHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.activity.ChartFilterType;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.embedded.AllocationDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.embedded.UserDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.service.AccountActivityService.CardAccountActivity;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.type.ChartData;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.permission.CustomUser;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import javax.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Slf4j
public class AccountActivityServiceTest extends BaseCapitalTest {

  @Autowired AccountService accountService;
  @Autowired AccountActivityService accountActivityService;
  @Autowired BusinessBankAccountService businessBankAccountService;
  @Autowired BusinessService businessService;
  @Autowired NetworkMessageService networkMessageService;

  @Autowired TestHelper testHelper;
  @Autowired AccountActivityRepository accountActivityRepository;
  @Autowired ExpenseCategoryService expenseCategoryService;
  @Autowired EntityManager entityManager;
  @Autowired PermissionValidationHelper permissionValidationHelper;
  @Autowired TestDataHelper testDataHelper;

  @Autowired ExpenseCategoryRepository expenseCategoryRepository;

  @Test
  void recordAccountActivityOnBusinessBankAccountTransaction() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    businessBankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
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
        false,
        true);
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
        createBusinessRecord.user().getId(),
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
        createBusinessRecord.user().getId(),
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
        false,
        true);
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
        createBusinessRecord.user().getId(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        parentAllocationRecord.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    UserService.CreateUpdateUserRecord user =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    testHelper.setCurrentUser(createBusinessRecord.user());
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
    entityManager.flush();

    testHelper.setCurrentUser(createBusinessRecord.user());

    final AccountActivityFilterCriteria criteria = new AccountActivityFilterCriteria();
    criteria.setBusinessId(business.getId());
    criteria.setPageToken(new PageToken(0, 10, null));

    Page<AccountActivity> accountActivity =
        accountActivityRepository.find(business.getId(), criteria);

    assertThat(accountActivity).hasSize(3);
  }

  @Test
  void retrieveAllAccountActivityFilterByAllocationType() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
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
        createBusinessRecord.user().getId(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        parentAllocationRecord.allocation().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(21)));
    entityManager.flush();

    final AccountActivityFilterCriteria criteria1 = new AccountActivityFilterCriteria();
    criteria1.setBusinessId(business.getId());
    criteria1.setTypes(List.of(AccountActivityType.REALLOCATE));
    criteria1.setPageToken(new PageToken(0, 10, null));

    Page<AccountActivity> withdrawalFilteredAccountActivity =
        accountActivityRepository.find(business.getId(), criteria1);

    assertThat(withdrawalFilteredAccountActivity).hasSize(2);

    final AccountActivityFilterCriteria criteria2 = new AccountActivityFilterCriteria();
    criteria2.setBusinessId(business.getId());
    criteria2.setTypes(List.of(AccountActivityType.BANK_DEPOSIT));
    criteria2.setPageToken(new PageToken(0, 10, null));

    Page<AccountActivity> depositFilteredAccountActivity =
        accountActivityRepository.find(business.getId(), criteria2);

    assertThat(depositFilteredAccountActivity).hasSize(1);
  }

  @Test
  void retrieveAllAccountActivityFilterByStatus() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(businessRecord.user());

    AccountActivity approvedAccountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(businessRecord.allocationRecord().allocation()),
            OffsetDateTime.now(),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);

    accountActivityRepository.saveAndFlush(approvedAccountActivity);

    final AccountActivityFilterCriteria criteria1 = new AccountActivityFilterCriteria();
    criteria1.setBusinessId(businessRecord.business().getId());
    criteria1.setPageToken(new PageToken(0, 10, null));
    criteria1.setStatuses(List.of(AccountActivityStatus.APPROVED));

    Page<AccountActivity> depositFilteredAccountActivity =
        accountActivityRepository.find(businessRecord.business().getId(), criteria1);

    assertThat(depositFilteredAccountActivity).hasSize(1);

    final AccountActivityFilterCriteria criteria2 = new AccountActivityFilterCriteria();
    criteria2.setBusinessId(businessRecord.business().getId());
    criteria2.setPageToken(new PageToken(0, 10, null));
    criteria2.setStatuses(List.of(AccountActivityStatus.CANCELED));

    Page<AccountActivity> canceledFilteredAccountActivity =
        accountActivityRepository.find(businessRecord.business().getId(), criteria2);

    assertThat(canceledFilteredAccountActivity).hasSize(0);
  }

  @Test
  void getUserAccountActivity() {
    assertThrows(
        RecordNotFoundException.class,
        () -> accountActivityService.findByReceiptId(new TypedId<>(), new TypedId<>()));
  }

  @SneakyThrows
  @Test
  void getAccountActivity_onlyActivityOwnersAndAllocationManagersCanAccessActivity() {
    CreateBusinessRecord primaryBusinessRecord = testHelper.createBusiness();

    testHelper.setCurrentUser(primaryBusinessRecord.user());
    User activityOwner =
        testHelper
            .createUserWithRole(
                primaryBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    final AccountActivityConfig config =
        AccountActivityConfig.fromCreateBusinessRecord(primaryBusinessRecord)
            .owner(activityOwner)
            .build();
    final AccountActivity accountActivity = testDataHelper.createAccountActivity(config);

    permissionValidationHelper
        .buildValidator(primaryBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .addRootAllocationCustomUser(CustomUser.pass(activityOwner))
        .build()
        .validateServiceMethod(
            () -> accountActivityService.getAccountActivity(accountActivity.getId()));
  }

  @SneakyThrows
  @Test
  void updateAccountActivity_onlyAccessibleToActivityOwnerAndManageFundsPermissions() {
    CreateBusinessRecord primaryBusinessRecord = testHelper.createBusiness();

    testHelper.setCurrentUser(primaryBusinessRecord.user());
    User activityOwner =
        testHelper
            .createUserWithRole(
                primaryBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    final AccountActivityConfig config =
        AccountActivityConfig.fromCreateBusinessRecord(primaryBusinessRecord)
            .owner(activityOwner)
            .build();
    final AccountActivity accountActivity = testDataHelper.createAccountActivity(config);

    final ThrowingRunnable action =
        () ->
            accountActivityService.updateAccountActivity(
                accountActivity, "I am updating this activity", null);

    permissionValidationHelper
        .buildValidator(primaryBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .addRootAllocationCustomUser(CustomUser.pass(activityOwner))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void updateAccountActivity_success() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(businessRecord.user());
    AccountActivity accountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(businessRecord.allocationRecord().allocation()),
            OffsetDateTime.now(),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    log.info("AccountActivity: {}", accountActivity);
    accountActivity.setNotes("");
    Integer iconRef = 0;

    ExpenseCategory expenseCategory =
        new ExpenseCategory(
            businessRecord.business().getId(), 0, "Fuel", ExpenseCategoryStatus.ACTIVE);

    expenseCategoryRepository.save(expenseCategory);

    if (null != iconRef && iconRef != 0) {
      accountActivity.setExpenseDetails(
          new ExpenseDetails(0, expenseCategory.getId(), expenseCategory.getCategoryName()));
    }
    accountActivityRepository.save(accountActivity);
    AccountActivity accountActivityResult =
        accountActivityRepository.findById(accountActivity.getId()).orElseThrow();
    log.info(String.valueOf(accountActivityResult));
  }

  @Test
  void updateAccountActivity_settingExpenseCategoryUpdatesIntegrationStatus() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(businessRecord.user());
    AccountActivity accountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(businessRecord.allocationRecord().allocation()),
            OffsetDateTime.now(),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setNotes("");
    accountActivity.setUser(UserDetails.of(businessRecord.user()));
    accountActivity = accountActivityRepository.save(accountActivity);
    testHelper.setCurrentUser(businessRecord.user());

    ExpenseCategory expenseCategory =
        new ExpenseCategory(
            businessRecord.business().getId(), 0, "Fuel", ExpenseCategoryStatus.ACTIVE);

    expenseCategoryRepository.save(expenseCategory);

    assertThat(
            accountActivityService.updateAccountActivity(
                accountActivity, "After Update", expenseCategory.getId()))
        .extracting(it -> it.getIntegrationSyncStatus())
        .isEqualTo(AccountActivityIntegrationSyncStatus.READY);
  }

  @Test
  void retrieveAccountActivity_UserPermissions() {
    final CreateBusinessRecord primaryBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(primaryBusinessRecord.user());
    User activityOwner =
        testHelper
            .createUserWithRole(
                primaryBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    final AccountActivityConfig config =
        AccountActivityConfig.fromCreateBusinessRecord(primaryBusinessRecord)
            .owner(activityOwner)
            .build();
    final AccountActivity accountActivity = testDataHelper.createAccountActivity(config);

    permissionValidationHelper
        .buildValidator(primaryBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .addRootAllocationCustomUser(CustomUser.pass(activityOwner))
        .build()
        .validateServiceMethod(
            () ->
                accountActivityService.retrieveAccountActivity(
                    primaryBusinessRecord.business().getId(), accountActivity.getId()));
  }

  @Test
  void getCardAccountActivity_UserFilterPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });

    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(createBusinessRecord.user())
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(createBusinessRecord.user())
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(createBusinessRecord.user())
            .cardId(card.getId())
            .build());

    entityManager.flush();

    final AccountActivityFilterCriteria criteria = new AccountActivityFilterCriteria();
    criteria.setFrom(OffsetDateTime.now().minusYears(1));
    criteria.setTo(OffsetDateTime.now().plusYears(1));
    final PageRequest pageRequest = new PageRequest(0, 10);
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final CardAccountActivity adminPage =
        accountActivityService.getCardAccountActivity(
            createBusinessRecord.business().getId(),
            createBusinessRecord.user().getId(),
            card.getId(),
            criteria);
    assertEquals(3, adminPage.activityPage().getTotalElements());
    assertEquals(3, adminPage.activityPage().getContent().size());

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final CardAccountActivity managerPage =
        accountActivityService.getCardAccountActivity(
            createBusinessRecord.business().getId(),
            createBusinessRecord.user().getId(),
            card.getId(),
            criteria);
    assertEquals(3, managerPage.activityPage().getTotalElements());
    assertEquals(3, managerPage.activityPage().getContent().size());

    // Other users don't matter, because this method is restricted based on the Card anyway
  }

  @Test
  void createCSVFile_UserFilterPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(users.employeeOwner())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());

    entityManager.flush();

    final AccountActivityFilterCriteria criteria = new AccountActivityFilterCriteria();
    criteria.setFrom(OffsetDateTime.now().minusYears(1));
    criteria.setTo(OffsetDateTime.now().plusYears(1));
    final PageRequest pageRequest = new PageRequest(0, 10);
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final byte[] adminBytes = accountActivityService.createCSVFile(criteria);
    assertEquals(4, new String(adminBytes).split("\n").length);

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final byte[] managerBytes = accountActivityService.createCSVFile(criteria);
    assertEquals(4, new String(managerBytes).split("\n").length);

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final byte[] otherEmployeeBytes = accountActivityService.createCSVFile(criteria);
    assertEquals(1, new String(otherEmployeeBytes).split("\n").length);

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final byte[] employeeOwnerBytes = accountActivityService.createCSVFile(criteria);
    assertEquals(2, new String(employeeOwnerBytes).split("\n").length);
  }

  @Test
  void getCardAccountActivity_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final AccountActivityFilterCriteria criteria = createCriteria();

    final ThrowingRunnable action =
        () ->
            accountActivityService.getCardAccountActivity(
                createBusinessRecord.business().getId(),
                createBusinessRecord.user().getId(),
                cardAndLimit.card().getId(),
                criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void findDataForChart_Merchant_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Howdy",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();

    final MerchantDetails merchant1 = testDataHelper.createMerchant();
    final MerchantDetails merchant2 = testDataHelper.createMerchant();
    merchant2.setName("OtherMerchant");

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(users.employeeOwner())
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().minusDays(1))
            .cardId(card.getId())
            .merchant(merchant1)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now())
            .cardId(card.getId())
            .merchant(merchant1)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .allocation(allocation)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().plusDays(1))
            .cardId(card.getId())
            .merchant(merchant2)
            .build());

    entityManager.flush();

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.MERCHANT,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final ChartData adminResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(adminResult.getAllocationChartData());
    assertNull(adminResult.getMerchantCategoryChartData());
    assertNull(adminResult.getUserChartData());
    assertEquals(2, adminResult.getMerchantChartData().size());
    assertTrue(
        adminResult.getMerchantChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        adminResult.getMerchantChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final ChartData managerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(managerResult.getMerchantCategoryChartData());
    assertNull(managerResult.getAllocationChartData());
    assertNull(managerResult.getUserChartData());
    assertEquals(2, managerResult.getMerchantChartData().size());
    assertTrue(
        managerResult.getMerchantChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        managerResult.getMerchantChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final ChartData otherEmployeeResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(otherEmployeeResult.getMerchantCategoryChartData());
    assertNull(otherEmployeeResult.getAllocationChartData());
    assertNull(otherEmployeeResult.getUserChartData());
    assertEquals(0, otherEmployeeResult.getMerchantChartData().size());

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final ChartData empOwnerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(empOwnerResult.getMerchantCategoryChartData());
    assertNull(empOwnerResult.getAllocationChartData());
    assertNull(empOwnerResult.getUserChartData());
    assertEquals(1, empOwnerResult.getMerchantChartData().size());
    assertEquals(
        new BigDecimal("1.00"),
        empOwnerResult.getMerchantChartData().get(0).getAmount().getAmount());
  }

  @Test
  void findDataForChart_MerchantCategory_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Howdy",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();

    final MerchantDetails merchant1 = testDataHelper.createMerchant();
    final MerchantDetails merchant2 = testDataHelper.createMerchant();
    merchant2.setName("OtherMerchant");
    merchant2.setType(MerchantType.ADVERTISING_SERVICES);

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(users.employeeOwner())
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().minusDays(1))
            .cardId(card.getId())
            .merchant(merchant1)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now())
            .cardId(card.getId())
            .merchant(merchant1)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .allocation(allocation)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().plusDays(1))
            .cardId(card.getId())
            .merchant(merchant2)
            .build());

    entityManager.flush();

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.MERCHANT_CATEGORY,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final ChartData adminResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(adminResult.getAllocationChartData());
    assertNull(adminResult.getMerchantChartData());
    assertNull(adminResult.getUserChartData());
    assertEquals(2, adminResult.getMerchantCategoryChartData().size());
    assertTrue(
        adminResult.getMerchantCategoryChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        adminResult.getMerchantCategoryChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final ChartData managerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(managerResult.getMerchantChartData());
    assertNull(managerResult.getAllocationChartData());
    assertNull(managerResult.getUserChartData());
    assertEquals(2, managerResult.getMerchantCategoryChartData().size());
    assertTrue(
        managerResult.getMerchantCategoryChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        managerResult.getMerchantCategoryChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final ChartData otherEmployeeResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(otherEmployeeResult.getMerchantChartData());
    assertNull(otherEmployeeResult.getAllocationChartData());
    assertNull(otherEmployeeResult.getUserChartData());
    assertEquals(0, otherEmployeeResult.getMerchantCategoryChartData().size());

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final ChartData empOwnerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(empOwnerResult.getMerchantChartData());
    assertNull(empOwnerResult.getAllocationChartData());
    assertNull(empOwnerResult.getUserChartData());
    assertEquals(1, empOwnerResult.getMerchantCategoryChartData().size());
    assertEquals(
        new BigDecimal("1.00"),
        empOwnerResult.getMerchantCategoryChartData().get(0).getAmount().getAmount());
  }

  @Test
  void findDataForChart_User_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Howdy",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(users.employeeOwner())
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().minusDays(1))
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(createBusinessRecord.user())
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now())
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(createBusinessRecord.user())
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().plusDays(1))
            .cardId(card.getId())
            .build());

    entityManager.flush();

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.EMPLOYEE,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final ChartData adminResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(adminResult.getMerchantCategoryChartData());
    assertNull(adminResult.getMerchantChartData());
    assertNull(adminResult.getAllocationChartData());
    assertEquals(2, adminResult.getUserChartData().size());
    assertTrue(
        adminResult.getUserChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        adminResult.getUserChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final ChartData managerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(managerResult.getMerchantCategoryChartData());
    assertNull(managerResult.getMerchantChartData());
    assertNull(managerResult.getAllocationChartData());
    assertEquals(2, managerResult.getUserChartData().size());
    assertTrue(
        managerResult.getUserChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        managerResult.getUserChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final ChartData otherEmployeeResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(otherEmployeeResult.getMerchantCategoryChartData());
    assertNull(otherEmployeeResult.getMerchantChartData());
    assertNull(otherEmployeeResult.getAllocationChartData());
    assertEquals(0, otherEmployeeResult.getUserChartData().size());

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final ChartData empOwnerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(empOwnerResult.getMerchantCategoryChartData());
    assertNull(empOwnerResult.getMerchantChartData());
    assertNull(empOwnerResult.getAllocationChartData());
    assertEquals(1, empOwnerResult.getUserChartData().size());
    assertEquals(
        new BigDecimal("1.00"), empOwnerResult.getUserChartData().get(0).getAmount().getAmount());
  }

  @Test
  void findDataForChart_Allocation_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Howdy",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(users.employeeOwner())
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().minusDays(1))
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now())
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .allocation(allocation)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().plusDays(1))
            .cardId(card.getId())
            .build());

    entityManager.flush();

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.ALLOCATION,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final ChartData adminResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(adminResult.getMerchantCategoryChartData());
    assertNull(adminResult.getMerchantChartData());
    assertNull(adminResult.getUserChartData());
    assertEquals(2, adminResult.getAllocationChartData().size());
    assertTrue(
        adminResult.getAllocationChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        adminResult.getAllocationChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final ChartData managerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(managerResult.getMerchantCategoryChartData());
    assertNull(managerResult.getMerchantChartData());
    assertNull(managerResult.getUserChartData());
    assertEquals(2, managerResult.getAllocationChartData().size());
    assertTrue(
        managerResult.getAllocationChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
    assertTrue(
        managerResult.getAllocationChartData().stream()
            .anyMatch(data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final ChartData otherEmployeeResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(otherEmployeeResult.getMerchantCategoryChartData());
    assertNull(otherEmployeeResult.getMerchantChartData());
    assertNull(otherEmployeeResult.getUserChartData());
    assertEquals(0, otherEmployeeResult.getAllocationChartData().size());

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final ChartData empOwnerResult =
        accountActivityService.findDataForChart(createBusinessRecord.business().getId(), criteria);
    assertNull(empOwnerResult.getMerchantCategoryChartData());
    assertNull(empOwnerResult.getMerchantChartData());
    assertNull(empOwnerResult.getUserChartData());
    assertEquals(1, empOwnerResult.getAllocationChartData().size());
    assertEquals(
        new BigDecimal("1.00"),
        empOwnerResult.getAllocationChartData().get(0).getAmount().getAmount());
  }

  @Test
  void findDataForLineGraph_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(users.employeeOwner())
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().minusDays(1))
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now())
            .cardId(card.getId())
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().plusDays(1))
            .cardId(card.getId())
            .build());

    entityManager.flush();

    final GraphFilterCriteria criteria =
        new GraphFilterCriteria(
            createBusinessRecord.allocationRecord().allocation().getId(),
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1));

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final DashboardData adminResult =
        accountActivityService.findDataForLineGraph(
            createBusinessRecord.business().getId(), criteria);
    assertEquals(10, adminResult.getGraphData().size());
    assertEquals(new BigDecimal("2.00"), adminResult.getGraphData().get(4).getAmount());
    assertEquals(new BigDecimal("1.00"), adminResult.getGraphData().get(5).getAmount());
    assertEquals(new BigDecimal("3.00"), adminResult.getTotalAmount());

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final DashboardData managerResult =
        accountActivityService.findDataForLineGraph(
            createBusinessRecord.business().getId(), criteria);
    assertEquals(10, managerResult.getGraphData().size());
    assertEquals(new BigDecimal("3.00"), managerResult.getTotalAmount());
    assertEquals(new BigDecimal("2.00"), managerResult.getGraphData().get(4).getAmount());
    assertEquals(new BigDecimal("1.00"), managerResult.getGraphData().get(5).getAmount());

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final DashboardData otherEmployeeResult =
        accountActivityService.findDataForLineGraph(
            createBusinessRecord.business().getId(), criteria);
    assertEquals(10, otherEmployeeResult.getGraphData().size());
    assertEquals(new BigDecimal("0"), otherEmployeeResult.getTotalAmount());

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final DashboardData employeeOwnerResult =
        accountActivityService.findDataForLineGraph(
            createBusinessRecord.business().getId(), criteria);
    assertEquals(10, employeeOwnerResult.getGraphData().size());
    assertEquals(new BigDecimal("1.00"), employeeOwnerResult.getTotalAmount());
    assertEquals(new BigDecimal("1.00"), employeeOwnerResult.getGraphData().get(4).getAmount());
  }

  private AccountActivityFilterCriteria createCriteria() {
    final PageRequest pageRequest = new PageRequest();
    pageRequest.setPageNumber(1);
    pageRequest.setPageSize(1);
    return new AccountActivityFilterCriteria(
        new TypedId<>(), List.of(), OffsetDateTime.now(), OffsetDateTime.now(), pageRequest);
  }

  record FindPermissionUsers(User employeeOwner, User otherEmployee, User admin, User manager) {}

  private MerchantDetails createMerchant(final int index) {
    final MerchantDetails merchantDetails = new MerchantDetails();
    merchantDetails.setType(MerchantType.ADVERTISING_SERVICES);
    merchantDetails.setName("MerchantName-%d".formatted(index));
    return merchantDetails;
  }

  @Test
  void find_Pagination() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());

    final List<AccountActivity> allActivities =
        IntStream.range(0, 20)
            .mapToObj(
                index ->
                    testDataHelper.createAccountActivity(
                        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                            .activityTime(OffsetDateTime.now().plusMinutes(index))
                            .merchant(createMerchant(index))
                            .build()))
            .sorted((a1, a2) -> a1.getActivityTime().compareTo(a2.getActivityTime()) * -1)
            .toList();

    final AccountActivityFilterCriteria criteria = new AccountActivityFilterCriteria();
    criteria.setFrom(OffsetDateTime.now().minusYears(1));
    criteria.setTo(OffsetDateTime.now().plusYears(1));
    criteria.setTypes(List.of(AccountActivityType.BANK_DEPOSIT));
    final PageRequest pageRequest = new PageRequest(0, 10);
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));

    final Page<AccountActivity> page1 =
        accountActivityService.find(createBusinessRecord.business().getId(), criteria);
    assertEquals(10, page1.getContent().size());
    assertEquals(20, page1.getTotalElements());
    assertThat(page1.getContent())
        .containsExactly(allActivities.subList(0, 10).toArray(new AccountActivity[10]));

    final PageRequest page2Request = new PageRequest(1, 10);
    criteria.setPageToken(PageRequest.toPageToken(page2Request));

    final Page<AccountActivity> page2 =
        accountActivityService.find(createBusinessRecord.business().getId(), criteria);
    assertEquals(10, page2.getContent().size());
    assertEquals(20, page2.getTotalElements());
    assertThat(page2.getContent())
        .containsExactly(allActivities.subList(10, 20).toArray(new AccountActivity[10]));
  }

  @Test
  void find_UserFilterPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final FindPermissionUsers users =
        testHelper.createWithCurrentUser(
            createBusinessRecord.user(),
            () -> {
              final User employeeOwner =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User otherEmployee =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_EMPLOYEE)
                      .user();
              final User admin =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_ADMIN)
                      .user();
              final User manager =
                  testHelper
                      .createUserWithRole(
                          createBusinessRecord.allocationRecord().allocation(),
                          DefaultRoles.ALLOCATION_MANAGER)
                      .user();

              return new FindPermissionUsers(employeeOwner, otherEmployee, admin, manager);
            });
    final AccountActivity activity1 =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(users.employeeOwner())
                .build());
    final AccountActivity activity2 =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final AccountActivity activity3 =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());

    entityManager.flush();

    final AccountActivityFilterCriteria criteria = new AccountActivityFilterCriteria();
    criteria.setFrom(OffsetDateTime.now().minusYears(1));
    criteria.setTo(OffsetDateTime.now().plusYears(1));
    final PageRequest pageRequest = new PageRequest(0, 10);
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));

    // Admin user should see all
    testHelper.setCurrentUser(users.admin());
    final Page<AccountActivity> adminPage =
        accountActivityService.find(createBusinessRecord.business().getId(), criteria);
    assertEquals(3, adminPage.getTotalElements());
    assertEquals(3, adminPage.getContent().size());
    assertThat(adminPage.getContent()).containsExactlyInAnyOrder(activity1, activity2, activity3);

    // Manager user should see all
    testHelper.setCurrentUser(users.manager());
    final Page<AccountActivity> managerPage =
        accountActivityService.find(createBusinessRecord.business().getId(), criteria);
    assertEquals(3, managerPage.getTotalElements());
    assertEquals(3, managerPage.getContent().size());
    assertThat(managerPage.getContent()).containsExactlyInAnyOrder(activity1, activity2, activity3);

    // Other Employee user should see none
    testHelper.setCurrentUser(users.otherEmployee());
    final Page<AccountActivity> otherEmployeePage =
        accountActivityService.find(createBusinessRecord.business().getId(), criteria);
    assertEquals(0, otherEmployeePage.getTotalElements());
    assertEquals(0, otherEmployeePage.getContent().size());

    // Employee owner should see self owned
    testHelper.setCurrentUser(users.employeeOwner());
    final Page<AccountActivity> selfOwnedPage =
        accountActivityService.find(createBusinessRecord.business().getId(), criteria);
    assertEquals(1, selfOwnedPage.getTotalElements());
    assertEquals(1, selfOwnedPage.getContent().size());
    assertThat(selfOwnedPage.getContent()).contains(activity1);
  }
}
