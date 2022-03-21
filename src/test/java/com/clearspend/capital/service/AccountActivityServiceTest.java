package com.clearspend.capital.service;

import static com.clearspend.capital.testutils.data.TestDataHelper.*;
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
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.permission.CustomUser;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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

    Page<AccountActivity> accountActivity =
        accountActivityRepository.find(
            business.getId(),
            new AccountActivityFilterCriteria(
                business.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new PageToken(0, 10, null)));

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
    entityManager.flush();

    Page<AccountActivity> withdrawalFilteredAccountActivity =
        accountActivityRepository.find(
            business.getId(),
            new AccountActivityFilterCriteria(
                business.getId(),
                null,
                null,
                null,
                List.of(AccountActivityType.REALLOCATE),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(withdrawalFilteredAccountActivity).hasSize(2);

    Page<AccountActivity> depositFilteredAccountActivity =
        accountActivityRepository.find(
            business.getId(),
            new AccountActivityFilterCriteria(
                business.getId(),
                null,
                null,
                null,
                List.of(AccountActivityType.BANK_DEPOSIT),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(depositFilteredAccountActivity).hasSize(1);
  }

  @Test
  void retrieveAllAccountActivityFilterByStatus() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(businessRecord.user());

    AccountActivity approvedAccountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().allocation().getId(),
            businessRecord.allocationRecord().allocation().getName(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);

    accountActivityRepository.saveAndFlush(approvedAccountActivity);

    Page<AccountActivity> depositFilteredAccountActivity =
        accountActivityRepository.find(
            businessRecord.business().getId(),
            new AccountActivityFilterCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(AccountActivityStatus.APPROVED),
                null,
                null,
                null,
                null,
                null,
                new PageToken(0, 10, null)));

    assertThat(depositFilteredAccountActivity).hasSize(1);

    Page<AccountActivity> canceledFilteredAccountActivity =
        accountActivityRepository.find(
            businessRecord.business().getId(),
            new AccountActivityFilterCriteria(
                businessRecord.business().getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(AccountActivityStatus.CANCELED),
                null,
                null,
                null,
                null,
                null,
                new PageToken(0, 10, null)));

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
            .ownerId(activityOwner.getId())
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
            .ownerId(activityOwner.getId())
            .build();
    final AccountActivity accountActivity = testDataHelper.createAccountActivity(config);

    final ThrowingRunnable action =
        () ->
            accountActivityService.updateAccountActivity(
                accountActivity, "I am updating this activity", Optional.empty());

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
            businessRecord.allocationRecord().allocation().getId(),
            businessRecord.allocationRecord().allocation().getName(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
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

  @Test
  void updateAccountActivity_settingExpenseCategoryUpdatesIntegrationStatus() {
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
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setNotes("");
    accountActivity.setUserId(businessRecord.user().getId());
    accountActivity = accountActivityRepository.save(accountActivity);
    testHelper.setCurrentUser(businessRecord.user());
    assertThat(
            accountActivityService.updateAccountActivity(
                accountActivity, "After Update", Optional.of(1)))
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
            .ownerId(activityOwner.getId())
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
  void retrieveAccountActivityByAdjustmentId_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final AdjustmentRecord adjustmentRecord =
        testDataHelper.createAdjustmentWithJoins(
            AdjustmentWithJoinsConfig.fromCreateBusinessRecord(createBusinessRecord).build());

    testHelper.setCurrentUser(createBusinessRecord.user());
    final User activityOwner =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    testDataHelper.createAccountActivity(
        AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
            .adjustmentId(adjustmentRecord.adjustment().getId())
            .ownerId(activityOwner.getId())
            .build());

    final ThrowingRunnable action =
        () ->
            accountActivityService.retrieveAccountActivityByAdjustmentId(
                createBusinessRecord.business().getId(), adjustmentRecord.adjustment().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .addRootAllocationCustomUser(CustomUser.pass(activityOwner))
        .build()
        .validateServiceMethod(action);
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

  @Disabled
  @Test
  void findDataForChart_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Allocation",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final ChartFilterCriteria criteriaWithAllocation =
        new ChartFilterCriteria(
            ChartFilterType.ALLOCATION,
            allocation.getId(),
            createBusinessRecord.user().getId(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            Sort.Direction.ASC);

    final ThrowingRunnable actionWithAllocation =
        () ->
            accountActivityService.findDataForChart(
                createBusinessRecord.business().getId(), criteriaWithAllocation);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(actionWithAllocation);

    final ChartFilterCriteria criteriaWithoutAllocation =
        new ChartFilterCriteria(
            ChartFilterType.ALLOCATION,
            null,
            createBusinessRecord.user().getId(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            Sort.Direction.ASC);

    final ThrowingRunnable actionWithoutAllocation =
        () ->
            accountActivityService.findDataForChart(
                createBusinessRecord.business().getId(), criteriaWithoutAllocation);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .build()
        .validateServiceMethod(actionWithoutAllocation);
  }

  @Disabled
  @Test
  void findDataForLineGraph_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Allocation",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final GraphFilterCriteria criteriaWithAllocation =
        new GraphFilterCriteria(
            allocation.getId(),
            createBusinessRecord.user().getId(),
            OffsetDateTime.now(),
            OffsetDateTime.now());

    final ThrowingRunnable actionWithAllocation =
        () ->
            accountActivityService.findDataForLineGraph(
                createBusinessRecord.business().getId(), criteriaWithAllocation);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(actionWithAllocation);

    final GraphFilterCriteria criteriaWithoutAllocation =
        new GraphFilterCriteria(
            null, createBusinessRecord.user().getId(), OffsetDateTime.now(), OffsetDateTime.now());

    final ThrowingRunnable actionWithoutAllocation =
        () ->
            accountActivityService.findDataForLineGraph(
                createBusinessRecord.business().getId(), criteriaWithoutAllocation);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .build()
        .validateServiceMethod(actionWithoutAllocation);
  }

  private AccountActivityFilterCriteria createCriteria() {
    final PageRequest pageRequest = new PageRequest();
    pageRequest.setPageNumber(1);
    pageRequest.setPageSize(1);
    return new AccountActivityFilterCriteria(
        new TypedId<>(), List.of(), OffsetDateTime.now(), OffsetDateTime.now(), pageRequest);
  }

  @Disabled
  @Test
  void find_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Allocation",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final AccountActivityFilterCriteria criteriaWithAllocation = createCriteria();
    criteriaWithAllocation.setAllocationId(allocation.getId());

    final ThrowingRunnable actionWithAllocation =
        () ->
            accountActivityService.find(
                createBusinessRecord.business().getId(), criteriaWithAllocation);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(actionWithAllocation);

    final AccountActivityFilterCriteria criteriaWithoutAllocation = createCriteria();

    final ThrowingRunnable actionWithoutAllocation =
        () ->
            accountActivityService.find(
                createBusinessRecord.business().getId(), criteriaWithoutAllocation);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .build()
        .validateServiceMethod(actionWithoutAllocation);
  }

  @Disabled
  @Test
  void createCSVFile_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final Allocation allocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Allocation",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final AccountActivityFilterCriteria criteriaWithAllocation = createCriteria();
    criteriaWithAllocation.setAllocationId(allocation.getId());

    final ThrowingRunnable actionWithAllocation =
        () -> accountActivityService.createCSVFile(criteriaWithAllocation);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(actionWithAllocation);

    final AccountActivityFilterCriteria criteriaWithoutAllocation = createCriteria();

    final ThrowingRunnable actionWithoutAllocation =
        () -> accountActivityService.createCSVFile(criteriaWithoutAllocation);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .setChildAllocation(allocation)
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .build()
        .validateServiceMethod(actionWithoutAllocation);
  }
}
