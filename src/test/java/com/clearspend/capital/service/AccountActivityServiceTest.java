package com.clearspend.capital.service;

import static com.clearspend.capital.testutils.data.TestDataHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
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
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.service.AccountActivityService.CardAccountActivity;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.type.ChartData;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.testutils.data.TestDataHelper;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Slf4j
public class AccountActivityServiceTest extends BaseCapitalTest {

  @Autowired AccountService accountService;
  @Autowired AccountActivityService accountActivityService;
  @Autowired BusinessBankAccountService businessBankAccountService;
  @Autowired BusinessService businessService;
  @Autowired ChartOfAccountsMappingRepository chartOfAccountsMappingRepository;
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
        false);
    Account rootAllocationAccount = createBusinessRecord.allocationRecord().account();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());
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
        false);
    Account rootAllocationAccount = createBusinessRecord.allocationRecord().account();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());
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
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
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
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());
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
    criteria2.setTypes(List.of(AccountActivityType.BANK_DEPOSIT_STRIPE));
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
            AccountActivityType.BANK_DEPOSIT_STRIPE,
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
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .allowUser(activityOwner)
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

    final ThrowingSupplier<AccountActivity> action =
        () ->
            accountActivityService.updateAccountActivity(
                accountActivity, "I am updating this activity", null);

    permissionValidationHelper
        .buildValidator(primaryBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .allowUser(activityOwner)
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
            AccountActivityType.BANK_DEPOSIT_STRIPE,
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
            businessRecord.business().getId(), 0, "Fuel", ExpenseCategoryStatus.ACTIVE, false);

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
  void updateAccountActivity_settingExpenseCategoryUpdatesIntegrationStatusWhenCategoryIsMapped() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(businessRecord.user());
    AccountActivity accountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT_STRIPE,
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

    // Add an Expense Category
    ExpenseCategory expenseCategory =
        new ExpenseCategory(
            businessRecord.business().getId(), 0, "Fuel", ExpenseCategoryStatus.ACTIVE, false);
    expenseCategory = expenseCategoryRepository.save(expenseCategory);

    // Make sure that the Expense Category is 'mapped' to a QBO Account
    testHelper.setCurrentUser(businessRecord.user());
    ChartOfAccountsMapping mapping = new ChartOfAccountsMapping();
    mapping.setBusinessId(businessRecord.businessOwner().getBusinessId());
    mapping.setExpenseCategoryId(expenseCategory.getId());
    mapping.setAccountRefId("Testing");
    mapping.setExpenseCategoryIconRef(42);
    chartOfAccountsMappingRepository.saveAndFlush(mapping);

    assertThat(
            accountActivityService.updateAccountActivity(
                accountActivity, "After Update", expenseCategory.getId()))
        .extracting(it -> it.getIntegrationSyncStatus())
        .isEqualTo(AccountActivityIntegrationSyncStatus.READY);
  }

  @Test
  void
      updateAccountActivity_settingExpenseCategoryDoesNotUpdatesIntegrationStatusWhenCategoryIsUnmapped() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(businessRecord.user());
    AccountActivity accountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT_STRIPE,
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

    // Add an Expense Category
    ExpenseCategory expenseCategory =
        new ExpenseCategory(
            businessRecord.business().getId(), 0, "Fuel", ExpenseCategoryStatus.ACTIVE, false);
    expenseCategory = expenseCategoryRepository.save(expenseCategory);

    // Don't map the Expense Category before the update. This should set the Expense Category but
    // not
    // transitively update the Integration Sync Status
    assertThat(
            accountActivityService.updateAccountActivity(
                accountActivity, "After Update", expenseCategory.getId()))
        .extracting(it -> it.getIntegrationSyncStatus())
        .isEqualTo(AccountActivityIntegrationSyncStatus.NOT_READY);
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
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .allowUser(activityOwner)
        .build()
        .validateServiceMethod(
            () ->
                accountActivityService.retrieveAccountActivity(
                    primaryBusinessRecord.business().getId(), accountActivity.getId()));
  }

  @Test
  void getCardAccountActivity_UserFilterPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
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

    final ThrowingSupplier<CardAccountActivity> action =
        () ->
            accountActivityService.getCardAccountActivity(
                createBusinessRecord.business().getId(),
                createBusinessRecord.user().getId(),
                card.getId(),
                criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .<CardAccountActivity>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER),
            result -> {
              assertEquals(3, result.activityPage().getTotalElements());
              assertEquals(3, result.activityPage().getContent().size());
            })
        .build()
        .validateServiceMethod(action);
  }

  private User createEmployeeOwnerUser(final CreateBusinessRecord createBusinessRecord) {
    return testHelper
        .createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE)
        .user();
  }

  private void prepareChartTestData(
      final CreateBusinessRecord createBusinessRecord, final User employeeOwner) {
    final User otherUser = testHelper.createUser(createBusinessRecord.business()).user();
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Howdy",
                createBusinessRecord.allocationRecord().allocation().getId())
            .allocation();

    final MerchantDetails merchant1 = testDataHelper.createMerchant();
    final MerchantDetails merchant2 = testDataHelper.createMerchant();
    merchant2.setName("OtherMerchant");
    merchant2.setType(MerchantType.ADVERTISING_SERVICES);

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(employeeOwner)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().minusDays(1))
            .cardId(card.getId())
            .merchant(merchant1)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(otherUser)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now())
            .cardId(card.getId())
            .merchant(merchant1)
            .build());
    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(otherUser)
            .allocation(allocation)
            .type(AccountActivityType.NETWORK_CAPTURE)
            .activityTime(OffsetDateTime.now().plusDays(1))
            .cardId(card.getId())
            .merchant(merchant2)
            .build());

    entityManager.flush();
  }

  @Test
  void findDataForChart_Merchant_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    prepareChartTestData(createBusinessRecord, employeeOwner);

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.MERCHANT,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    final ThrowingSupplier<ChartData> action =
        () ->
            accountActivityService.findDataForChart(
                createBusinessRecord.business().getId(), criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .<ChartData>allowAllGlobalRolesWithResult(
            globalRoleResult -> {
              assertNull(globalRoleResult.getMerchantCategoryChartData());
              assertNull(globalRoleResult.getAllocationChartData());
              assertNull(globalRoleResult.getUserChartData());
              assertEquals(0, globalRoleResult.getMerchantChartData().size());
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_ADMIN,
            adminResult -> {
              assertNull(adminResult.getAllocationChartData());
              assertNull(adminResult.getMerchantCategoryChartData());
              assertNull(adminResult.getUserChartData());
              assertEquals(2, adminResult.getMerchantChartData().size());
              assertTrue(
                  adminResult.getMerchantChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  adminResult.getMerchantChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_MANAGER,
            managerResult -> {
              assertNull(managerResult.getMerchantCategoryChartData());
              assertNull(managerResult.getAllocationChartData());
              assertNull(managerResult.getUserChartData());
              assertEquals(2, managerResult.getMerchantChartData().size());
              assertTrue(
                  managerResult.getMerchantChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  managerResult.getMerchantChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY),
            otherEmployeeResult -> {
              assertNull(otherEmployeeResult.getMerchantCategoryChartData());
              assertNull(otherEmployeeResult.getAllocationChartData());
              assertNull(otherEmployeeResult.getUserChartData());
              assertEquals(0, otherEmployeeResult.getMerchantChartData().size());
            })
        .<ChartData>allowUserWithResult(
            employeeOwner,
            empOwnerResult -> {
              assertNull(empOwnerResult.getMerchantCategoryChartData());
              assertNull(empOwnerResult.getAllocationChartData());
              assertNull(empOwnerResult.getUserChartData());
              assertEquals(1, empOwnerResult.getMerchantChartData().size());
              assertEquals(
                  new BigDecimal("1.00"),
                  empOwnerResult.getMerchantChartData().get(0).getAmount().getAmount());
            })
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void findDataForChart_MerchantCategory_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    prepareChartTestData(createBusinessRecord, employeeOwner);

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.MERCHANT_CATEGORY,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    final ThrowingSupplier<ChartData> action =
        () ->
            accountActivityService.findDataForChart(
                createBusinessRecord.business().getId(), criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .<ChartData>allowAllGlobalRolesWithResult(
            globalRoleResult -> {
              assertNull(globalRoleResult.getMerchantChartData());
              assertNull(globalRoleResult.getAllocationChartData());
              assertNull(globalRoleResult.getUserChartData());
              assertEquals(0, globalRoleResult.getMerchantCategoryChartData().size());
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_ADMIN,
            adminResult -> {
              assertNull(adminResult.getAllocationChartData());
              assertNull(adminResult.getMerchantChartData());
              assertNull(adminResult.getUserChartData());
              assertEquals(2, adminResult.getMerchantCategoryChartData().size());
              assertTrue(
                  adminResult.getMerchantCategoryChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  adminResult.getMerchantCategoryChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_MANAGER,
            managerResult -> {
              assertNull(managerResult.getMerchantChartData());
              assertNull(managerResult.getAllocationChartData());
              assertNull(managerResult.getUserChartData());
              assertEquals(2, managerResult.getMerchantCategoryChartData().size());
              assertTrue(
                  managerResult.getMerchantCategoryChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  managerResult.getMerchantCategoryChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY),
            otherEmployeeResult -> {
              assertNull(otherEmployeeResult.getMerchantChartData());
              assertNull(otherEmployeeResult.getAllocationChartData());
              assertNull(otherEmployeeResult.getUserChartData());
              assertEquals(0, otherEmployeeResult.getMerchantCategoryChartData().size());
            })
        .<ChartData>allowUserWithResult(
            employeeOwner,
            empOwnerResult -> {
              assertNull(empOwnerResult.getMerchantChartData());
              assertNull(empOwnerResult.getAllocationChartData());
              assertNull(empOwnerResult.getUserChartData());
              assertEquals(1, empOwnerResult.getMerchantCategoryChartData().size());
              assertEquals(
                  new BigDecimal("1.00"),
                  empOwnerResult.getMerchantCategoryChartData().get(0).getAmount().getAmount());
            })
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void findDataForChart_User_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    prepareChartTestData(createBusinessRecord, employeeOwner);

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.EMPLOYEE,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    final ThrowingSupplier<ChartData> action =
        () ->
            accountActivityService.findDataForChart(
                createBusinessRecord.business().getId(), criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .<ChartData>allowAllGlobalRolesWithResult(
            globalRoleResult -> {
              assertNull(globalRoleResult.getMerchantCategoryChartData());
              assertNull(globalRoleResult.getAllocationChartData());
              assertNull(globalRoleResult.getMerchantChartData());
              assertEquals(0, globalRoleResult.getUserChartData().size());
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_ADMIN,
            adminResult -> {
              assertNull(adminResult.getAllocationChartData());
              assertNull(adminResult.getMerchantCategoryChartData());
              assertNull(adminResult.getMerchantChartData());
              assertEquals(2, adminResult.getUserChartData().size());
              assertTrue(
                  adminResult.getUserChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  adminResult.getUserChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_MANAGER,
            managerResult -> {
              assertNull(managerResult.getMerchantCategoryChartData());
              assertNull(managerResult.getAllocationChartData());
              assertNull(managerResult.getMerchantChartData());
              assertEquals(2, managerResult.getUserChartData().size());
              assertTrue(
                  managerResult.getUserChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  managerResult.getUserChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY),
            otherEmployeeResult -> {
              assertNull(otherEmployeeResult.getMerchantCategoryChartData());
              assertNull(otherEmployeeResult.getAllocationChartData());
              assertNull(otherEmployeeResult.getMerchantChartData());
              assertEquals(0, otherEmployeeResult.getUserChartData().size());
            })
        .<ChartData>allowUserWithResult(
            employeeOwner,
            empOwnerResult -> {
              assertNull(empOwnerResult.getMerchantCategoryChartData());
              assertNull(empOwnerResult.getAllocationChartData());
              assertNull(empOwnerResult.getMerchantChartData());
              assertEquals(1, empOwnerResult.getUserChartData().size());
              assertEquals(
                  new BigDecimal("1.00"),
                  empOwnerResult.getUserChartData().get(0).getAmount().getAmount());
            })
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void findDataForChart_Allocation_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    prepareChartTestData(createBusinessRecord, employeeOwner);

    final ChartFilterCriteria criteria =
        new ChartFilterCriteria(
            ChartFilterType.ALLOCATION,
            null,
            null,
            OffsetDateTime.now().minusWeeks(1),
            OffsetDateTime.now().plusWeeks(1),
            Sort.Direction.ASC);

    final ThrowingSupplier<ChartData> action =
        () ->
            accountActivityService.findDataForChart(
                createBusinessRecord.business().getId(), criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .<ChartData>allowAllGlobalRolesWithResult(
            globalRoleResult -> {
              assertNull(globalRoleResult.getMerchantCategoryChartData());
              assertNull(globalRoleResult.getMerchantChartData());
              assertNull(globalRoleResult.getUserChartData());
              assertEquals(0, globalRoleResult.getAllocationChartData().size());
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_ADMIN,
            adminResult -> {
              assertNull(adminResult.getMerchantChartData());
              assertNull(adminResult.getMerchantCategoryChartData());
              assertNull(adminResult.getUserChartData());
              assertEquals(2, adminResult.getAllocationChartData().size());
              assertTrue(
                  adminResult.getAllocationChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  adminResult.getAllocationChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_MANAGER,
            managerResult -> {
              assertNull(managerResult.getMerchantCategoryChartData());
              assertNull(managerResult.getMerchantChartData());
              assertNull(managerResult.getUserChartData());
              assertEquals(2, managerResult.getAllocationChartData().size());
              assertTrue(
                  managerResult.getAllocationChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("2.00"))));
              assertTrue(
                  managerResult.getAllocationChartData().stream()
                      .anyMatch(
                          data -> data.getAmount().getAmount().equals(new BigDecimal("1.00"))));
            })
        .<ChartData>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY),
            otherEmployeeResult -> {
              assertNull(otherEmployeeResult.getMerchantCategoryChartData());
              assertNull(otherEmployeeResult.getMerchantChartData());
              assertNull(otherEmployeeResult.getUserChartData());
              assertEquals(0, otherEmployeeResult.getAllocationChartData().size());
            })
        .<ChartData>allowUserWithResult(
            employeeOwner,
            empOwnerResult -> {
              assertNull(empOwnerResult.getMerchantCategoryChartData());
              assertNull(empOwnerResult.getMerchantChartData());
              assertNull(empOwnerResult.getUserChartData());
              assertEquals(1, empOwnerResult.getAllocationChartData().size());
              assertEquals(
                  new BigDecimal("1.00"),
                  empOwnerResult.getAllocationChartData().get(0).getAmount().getAmount());
            })
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void findDataForLineGraph_FullTestWithUserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    final CardAndLimit cardAndLimit =
        testDataHelper.createCardAndLimit(
            CardConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final Card card = cardAndLimit.card();

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .owner(employeeOwner)
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

    final ThrowingSupplier<DashboardData> action =
        () ->
            accountActivityService.findDataForLineGraph(
                createBusinessRecord.business().getId(), criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .<DashboardData>allowAllGlobalRolesWithResult(
            result -> {
              assertEquals(10, result.getGraphData().size());
              assertEquals(new BigDecimal("0"), result.getTotalAmount());
            })
        .<DashboardData>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER),
            result -> {
              assertEquals(10, result.getGraphData().size());
              assertEquals(new BigDecimal("2.00"), result.getGraphData().get(4).getAmount());
              assertEquals(new BigDecimal("1.00"), result.getGraphData().get(5).getAmount());
              assertEquals(new BigDecimal("3.00"), result.getTotalAmount());
            })
        .<DashboardData>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY),
            result -> {
              assertEquals(10, result.getGraphData().size());
              assertEquals(new BigDecimal("0"), result.getTotalAmount());
            })
        .<DashboardData>allowUserWithResult(
            employeeOwner,
            result -> {
              assertEquals(10, result.getGraphData().size());
              assertEquals(new BigDecimal("1.00"), result.getTotalAmount());
              assertEquals(new BigDecimal("1.00"), result.getGraphData().get(4).getAmount());
            })
        .build()
        .validateServiceMethod(action);
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
  void find_WithGlobalRoles() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setUserAsMaster(createBusinessRecord.user());
    final User customerServiceManager =
        testHelper
            .createUserWithGlobalRole(
                createBusinessRecord.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER)
            .user();
    testHelper.setCurrentUser(customerServiceManager);

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
    criteria.setTypes(List.of(AccountActivityType.BANK_DEPOSIT_STRIPE));
    final PageRequest pageRequest = new PageRequest(0, 10);
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));

    assertDoesNotThrow(
        () -> accountActivityService.find(createBusinessRecord.business().getId(), criteria));
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
    criteria.setTypes(List.of(AccountActivityType.BANK_DEPOSIT_STRIPE));
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
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    final AccountActivity activity1 =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(employeeOwner)
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

    final ThrowingSupplier<Page<AccountActivity>> action =
        () -> accountActivityService.find(createBusinessRecord.business().getId(), criteria);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .<Page<AccountActivity>>allowAllGlobalRolesWithResult(
            result -> {
              assertEquals(0, result.getTotalElements());
              assertEquals(0, result.getContent().size());
            })
        .<Page<AccountActivity>>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER),
            result -> {
              assertEquals(3, result.getTotalElements());
              assertEquals(3, result.getContent().size());
              assertThat(result.getContent())
                  .containsExactlyInAnyOrder(activity1, activity2, activity3);
            })
        .<Page<AccountActivity>>allowRolesOnAllocationWithResult(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY),
            result -> {
              assertEquals(0, result.getTotalElements());
              assertEquals(0, result.getContent().size());
            })
        .<Page<AccountActivity>>allowUserWithResult(
            employeeOwner,
            result -> {
              assertEquals(1, result.getTotalElements());
              assertEquals(1, result.getContent().size());
              assertThat(result.getContent()).contains(activity1);
            })
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void unlockSyncedTransaction_whenAssignedCategoryAndNot() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    final AccountActivity activity =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(employeeOwner)
                .syncStatus(AccountActivityIntegrationSyncStatus.SYNCED_LOCKED)
                .build());
    accountActivityRepository.save(activity);

    accountActivityService.unlockAccountActivityForSync(
        createBusinessRecord.businessOwner().getBusinessId(), activity.getId());
    assertThat(
            accountActivityService.getAccountActivity(activity.getId()).getIntegrationSyncStatus())
        .isEqualTo(AccountActivityIntegrationSyncStatus.NOT_READY);

    ExpenseCategory mappedCategory =
        new ExpenseCategory(
            createBusinessRecord.user().getBusinessId(),
            0,
            "My Category",
            ExpenseCategoryStatus.ACTIVE,
            false);
    expenseCategoryRepository.save(mappedCategory);

    ChartOfAccountsMapping mapping =
        new ChartOfAccountsMapping(
            createBusinessRecord.user().getBusinessId(), mappedCategory.getId(), 0, "0");
    chartOfAccountsMappingRepository.save(mapping);

    final AccountActivity mappedActivity =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(employeeOwner)
                .syncStatus(AccountActivityIntegrationSyncStatus.SYNCED_LOCKED)
                .expenseDetails(new ExpenseDetails(0, mappedCategory.getId(), "My Category"))
                .build());

    accountActivityService.unlockAccountActivityForSync(
        createBusinessRecord.businessOwner().getBusinessId(), mappedActivity.getId());
    assertThat(
            accountActivityService
                .getAccountActivity(mappedActivity.getId())
                .getIntegrationSyncStatus())
        .isEqualTo(AccountActivityIntegrationSyncStatus.READY);
  }

  @Test
  public void testCodatSupplierUpdate() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User employeeOwner = createEmployeeOwnerUser(createBusinessRecord);
    prepareChartTestData(createBusinessRecord, employeeOwner);
    final AccountActivityFilterCriteria criteria1 = new AccountActivityFilterCriteria();
    criteria1.setBusinessId(createBusinessRecord.business().getId());
    criteria1.setPageToken(new PageToken(0, 10, null));
    Page<AccountActivity> accountActivity =
        accountActivityRepository.find(createBusinessRecord.business().getId(), criteria1);
    AccountActivity firstOne = accountActivity.getContent().get(0);
    AccountActivity result =
        accountActivityService.updateCodatSupplier(firstOne.getId(), "10", "Walmart");
    assertThat(result.getMerchant().getCodatSupplierId().equals("10")).isTrue();
    assertThat(result.getMerchant().getCodatSupplierName().equals("Walmart")).isTrue();
  }
}
