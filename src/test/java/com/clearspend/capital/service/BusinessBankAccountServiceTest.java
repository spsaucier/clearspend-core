package com.clearspend.capital.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.business.BusinessBankAccountBalanceRepository;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class BusinessBankAccountServiceTest extends BaseCapitalTest {
  private static final Set<String> rootAllocationFailingRoles =
      Set.of(
          DefaultRoles.ALLOCATION_MANAGER,
          DefaultRoles.ALLOCATION_EMPLOYEE,
          DefaultRoles.ALLOCATION_VIEW_ONLY);
  private static final Set<String> childAllocationFailingRoles =
      Set.of(
          DefaultRoles.ALLOCATION_ADMIN,
          DefaultRoles.ALLOCATION_MANAGER,
          DefaultRoles.ALLOCATION_EMPLOYEE,
          DefaultRoles.ALLOCATION_VIEW_ONLY);

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessBankAccountService bankAccountService;
  @Autowired private PlaidClient plaidClient;
  @Autowired private BusinessBankAccountBalanceRepository businessBankAccountBalanceRepository;
  @Autowired private PermissionValidationHelper permissionValidationHelper;
  @Autowired private TestDataHelper testDataHelper;
  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
  }

  @Test
  void depositFunds() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            true);
  }

  @Test
  void withdrawFunds_success() {
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    testHelper.setCurrentUser(createBusinessRecord.user());

    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("700.51")),
        false);
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.WITHDRAW,
            Amount.of(Currency.USD, new BigDecimal("241.85")),
            true);
  }

  @Test
  void withdrawFunds_insufficientBalance() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("710.51")),
        true);

    InsufficientFundsException insufficientFundsException =
        assertThrows(
            InsufficientFundsException.class,
            () ->
                bankAccountService.transactBankAccount(
                    createBusinessRecord.business().getId(),
                    businessBankAccount.getId(),
                    createBusinessRecord.user().getId(),
                    BankAccountTransactType.WITHDRAW,
                    Amount.of(Currency.USD, new BigDecimal("1.85")),
                    true));
  }

  @Test
  void depositFunds_insufficientFunds() {
    assumeTrue(plaidClient.isConfigured());
    testHelper.setCurrentUser(createBusinessRecord.user());

    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    InsufficientFundsException insufficientFundsException =
        assertThrows(
            InsufficientFundsException.class,
            () ->
                bankAccountService.transactBankAccount(
                    createBusinessRecord.business().getId(),
                    businessBankAccount.getId(),
                    createBusinessRecord.user().getId(),
                    BankAccountTransactType.DEPOSIT,
                    Amount.of(Currency.USD, new BigDecimal("15000.00")),
                    true));
  }

  @Test
  void transactBankAccount_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    final Amount amount = new Amount();
    amount.setCurrency(Currency.USD);
    amount.setAmount(new BigDecimal(10));
    final ThrowingRunnable action =
        () ->
            bankAccountService.transactBankAccount(
                createBusinessRecord.business().getId(),
                businessBankAccount.getId(),
                createBusinessRecord.user().getId(),
                BankAccountTransactType.DEPOSIT,
                amount,
                true);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getBusinessBankAccounts() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    final List<BusinessBankAccount> bankAccounts =
        bankAccountService.getBusinessBankAccounts(createBusinessRecord.business().getId(), false);
    assertEquals(2, bankAccounts.size());
    assertTrue(bankAccounts.contains(businessBankAccount));
  }

  @Test
  void getBusinessBankAccounts_UserPermissions() {
    final ThrowingRunnable action =
        () ->
            bankAccountService.getBusinessBankAccounts(
                createBusinessRecord.business().getId(), false);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void createBusinessBankAccount() {
    final String routingNumber = "123";
    final String accountNumber = "456";
    final String accountName = "Account";
    final String accessToken = "Token";
    final String accountRef = "Ref";

    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount businessBankAccount =
        bankAccountService.createBusinessBankAccount(
            routingNumber,
            accountNumber,
            accountName,
            accessToken,
            accountRef,
            createBusinessRecord.business().getId());
    assertThat(
        businessBankAccount,
        allOf(
            hasProperty("businessId", equalTo(createBusinessRecord.business().getId())),
            hasProperty(
                "routingNumber", equalTo(new RequiredEncryptedStringWithHash(routingNumber))),
            hasProperty(
                "accountNumber", equalTo(new RequiredEncryptedStringWithHash(accountNumber))),
            hasProperty("accessToken", equalTo(new RequiredEncryptedStringWithHash(accessToken))),
            hasProperty(
                "plaidAccountRef", equalTo(new RequiredEncryptedStringWithHash(accountRef))),
            hasProperty("name", equalTo(accountName))));
  }

  @Test
  void createBusinessBankAccount_UserPermissions() {
    final String routingNumber = "123";
    final String accountNumber = "456";
    final String accountName = "Account";
    final String accessToken = "Token";
    final String accountRef = "Ref";
    final ThrowingRunnable action =
        () ->
            bankAccountService.createBusinessBankAccount(
                routingNumber,
                accountNumber,
                accountName,
                accessToken,
                accountRef,
                createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getLinkToken() throws Exception {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final String token = bankAccountService.getLinkToken(createBusinessRecord.business().getId());
    assertEquals("link-token-mock-%s".formatted(createBusinessRecord.business().getId()), token);
  }

  @Test
  void getLinkToken_UserPermissions() {
    final ThrowingRunnable action =
        () -> bankAccountService.getLinkToken(createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @SneakyThrows
  @Test
  void testLinkBusinessBankAccounts() {
    assumeTrue(plaidClient.isConfigured());
    testHelper.setCurrentUser(createBusinessRecord.user());
    int balancesBeforeTest = businessBankAccountBalanceRepository.findAll().size();

    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();

    // Set up to capture the log
    Logger accountLogger = (Logger) LoggerFactory.getLogger(BusinessBankAccountService.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    accountLogger.addAppender(listAppender);

    // under test
    List<BusinessBankAccount> linked =
        bankAccountService.linkBusinessBankAccounts(
            bankAccountService.getLinkToken(businessId), businessId);

    // Since we didn't mock the user to match the Plaid sandbox, there should be a
    // validation error for each account in the logs, showing that name and zip don't match.
    List<String> messages =
        listAppender.list.stream()
            .map(ILoggingEvent::getMessage)
            .filter(m -> m.contains("Validation failed for Plaid account ref ending "))
            .collect(Collectors.toList());

    assertEquals(2, messages.size());
    assertTrue(
        messages.stream()
            .allMatch(
                m -> m.endsWith("ValidationResult[namesMatch=false, postalCodesMatch=false]")));

    // Check that balances are recorded
    TypedId<BusinessBankAccountId> businessBankAccountId = linked.get(0).getId();
    Optional<BusinessBankAccountBalance> optionalBalance =
        businessBankAccountBalanceRepository.findFirstByBusinessBankAccountIdOrderByCreatedDesc(
            businessBankAccountId);
    assertTrue(businessBankAccountBalanceRepository.findAll().size() > balancesBeforeTest);
    BusinessBankAccountBalance balance = optionalBalance.orElseThrow(NoSuchElementException::new);

    Assertions.assertTrue(balance.getAvailable().isPositive());
    Assertions.assertTrue(balance.getCurrent().isPositive());
    assertNull(balance.getLimit());
  }

  @Test
  void linkBusinessBankAccounts_UserPermissions() {
    final ThrowingRunnable action =
        () ->
            bankAccountService.linkBusinessBankAccounts(
                "link-token-mock-LinkToken", createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void processBankAccountWithdrawFailure_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    final Amount amount = new Amount();
    amount.setCurrency(Currency.USD);
    amount.setAmount(new BigDecimal(10));
    final ThrowingRunnable action =
        () ->
            bankAccountService.processBankAccountWithdrawFailure(
                createBusinessRecord.business().getId(),
                businessBankAccount.getId(),
                amount,
                Collections.emptyList());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void registerExternalBank_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount bankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    final ThrowingRunnable action =
        () ->
            bankAccountService.registerExternalBank(
                createBusinessRecord.business().getId(),
                bankAccount.getId(),
                "IP Address",
                "User Agent");
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void retrieveBusinessBankAccount() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount bankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    final BusinessBankAccount result =
        bankAccountService.retrieveBusinessBankAccount(bankAccount.getId());
    assertEquals(bankAccount, result);
  }

  @Test
  void retrieveBusinessBankAccount_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount bankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    final ThrowingRunnable action =
        () -> bankAccountService.retrieveBusinessBankAccount(bankAccount.getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }
}
