package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.webhook.controller.StripeConnectHandlerAccessor;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.OperationLimitViolationException;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.business.BusinessSettings;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedStringWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.business.AccountLinkStatus;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.decline.LimitExceeded;
import com.clearspend.capital.data.model.decline.OperationLimitExceeded;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AchFundsAvailabilityMode;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountBalanceRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class BusinessBankAccountServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;
  @Autowired private BusinessBankAccountService bankAccountService;
  @Autowired private AccountService accountService;
  @Autowired private BusinessSettingsService businessSettingsService;
  @Autowired private StripeConnectHandlerAccessor stripeConnectHandlerAccessor;
  @Autowired private PlaidClient plaidClient;
  @Autowired private BusinessBankAccountBalanceRepository businessBankAccountBalanceRepository;
  @Autowired private PermissionValidationHelper permissionValidationHelper;
  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private BusinessBankAccountRepository businessBankAccountRepository;
  @Autowired private StripeMockClient stripeMockClient;

  private CreateBusinessRecord createBusinessRecord;
  private Allocation childAllocation;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    childAllocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child",
                createBusinessRecord.allocationRecord().allocation().getId())
            .allocation();
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

    AccountActivity declinedActivity =
        accountActivityRepository.findAll().stream()
            .filter(a -> a.getStatus() == AccountActivityStatus.DECLINED)
            .findFirst()
            .orElseThrow();
    assertThat(declinedActivity.getDeclineDetails())
        .containsOnly(new DeclineDetails(DeclineReason.INSUFFICIENT_FUNDS));
  }

  @Test
  void withdrawFunds_operationLimitViolation() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);

    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("10000")),
        false);

    for (int i = 0; i < 2; i++) {
      bankAccountService.transactBankAccount(
          createBusinessRecord.business().getId(),
          businessBankAccount.getId(),
          createBusinessRecord.user().getId(),
          BankAccountTransactType.WITHDRAW,
          Amount.of(Currency.USD, new BigDecimal("1.85")),
          true);
    }

    OperationLimitViolationException operationLimitViolationException =
        assertThrows(
            OperationLimitViolationException.class,
            () ->
                bankAccountService.transactBankAccount(
                    createBusinessRecord.business().getId(),
                    businessBankAccount.getId(),
                    createBusinessRecord.user().getId(),
                    BankAccountTransactType.WITHDRAW,
                    Amount.of(Currency.USD, new BigDecimal("1.85")),
                    true));

    AccountActivity declinedActivity =
        accountActivityRepository.findAll().stream()
            .filter(a -> a.getStatus() == AccountActivityStatus.DECLINED)
            .findFirst()
            .orElseThrow();
    assertThat(declinedActivity.getDeclineDetails())
        .containsOnly(OperationLimitExceeded.from(operationLimitViolationException));
  }

  @Test
  void withdrawFunds_limitViolation() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);

    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("10000")),
        false);

    LimitViolationException limitViolationException =
        assertThrows(
            LimitViolationException.class,
            () ->
                bankAccountService.transactBankAccount(
                    createBusinessRecord.business().getId(),
                    businessBankAccount.getId(),
                    createBusinessRecord.user().getId(),
                    BankAccountTransactType.DEPOSIT,
                    Amount.of(Currency.USD, new BigDecimal("100")),
                    false));

    AccountActivity declinedActivity =
        accountActivityRepository.findAll().stream()
            .filter(a -> a.getStatus() == AccountActivityStatus.DECLINED)
            .findFirst()
            .orElseThrow();
    assertThat(declinedActivity.getDeclineDetails())
        .containsOnly(LimitExceeded.from(limitViolationException));
  }

  @Test
  void depositFunds_AchFundsAvailability_Standard() {
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);
    businessSettingsService.updateBusinessSettings(
        businessId,
        BusinessSettings.builder()
            .achFundsAvailabilityMode(AchFundsAvailabilityMode.STANDARD)
            .build());

    Amount amount = Amount.of(Currency.USD, new BigDecimal("1000"));

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.DEPOSIT,
            amount,
            true);

    Account account =
        accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(adjustmentAndHoldRecord.hold().getExpirationDate())
        .isAfter(OffsetDateTime.now(Clock.systemUTC()).plusDays(4));
    assertThat(account.getAvailableBalance()).isEqualTo(Amount.of(Currency.USD, BigDecimal.ZERO));

    sendAchTransferCompletion(
        businessBankAccount.getId(),
        adjustmentAndHoldRecord.hold().getId(),
        adjustmentAndHoldRecord.adjustment().getId(),
        amount);

    // hold should still be in place and ledger balance = 0
    account = accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(adjustmentAndHoldRecord.hold().getExpirationDate())
        .isAfter(OffsetDateTime.now(Clock.systemUTC()).plusDays(4));
    assertThat(account.getAvailableBalance()).isEqualTo(Amount.of(Currency.USD, BigDecimal.ZERO));
  }

  @Test
  void depositFunds_AchFundsAvailability_Fast() {
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);
    businessSettingsService.updateBusinessSettings(
        businessId,
        BusinessSettings.builder().achFundsAvailabilityMode(AchFundsAvailabilityMode.FAST).build());

    Amount amount = Amount.of(Currency.USD, new BigDecimal("1000"));

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.DEPOSIT,
            amount,
            true);

    Account account =
        accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(adjustmentAndHoldRecord.hold().getExpirationDate())
        .isAfter(OffsetDateTime.now(Clock.systemUTC()).plusDays(4));
    assertThat(account.getAvailableBalance()).isEqualTo(Amount.of(Currency.USD, BigDecimal.ZERO));

    sendAchTransferCompletion(
        businessBankAccount.getId(),
        adjustmentAndHoldRecord.hold().getId(),
        adjustmentAndHoldRecord.adjustment().getId(),
        amount);

    // hold should be released and funds available
    account = accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.RELEASED);
    assertThat(account.getAvailableBalance())
        .isEqualTo(Amount.of(Currency.USD, BigDecimal.valueOf(1000)));
  }

  @Test
  void depositFunds_AchFundsAvailability_Immediate() {
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);
    businessSettingsService.updateBusinessSettings(
        businessId,
        BusinessSettings.builder()
            .achFundsAvailabilityMode(AchFundsAvailabilityMode.IMMEDIATE)
            .immediateAchFundsLimit(BigDecimal.valueOf(1000))
            .build());

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(1000));
    stripeMockClient.setClearspendFinancialAccountBalance(Amount.of(Currency.USD, 2000L));

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.DEPOSIT,
            amount,
            true);

    Account account =
        accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(adjustmentAndHoldRecord.hold().getExpirationDate())
        .isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(account.getAvailableBalance())
        .isEqualTo(Amount.of(Currency.USD, BigDecimal.valueOf(1000)));

    sendAchTransferCompletion(
        businessBankAccount.getId(),
        adjustmentAndHoldRecord.hold().getId(),
        adjustmentAndHoldRecord.adjustment().getId(),
        amount);

    // hold should be released and funds available
    account = accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.RELEASED);
    assertThat(account.getAvailableBalance())
        .isEqualTo(Amount.of(Currency.USD, BigDecimal.valueOf(1000)));
  }

  @Test
  void depositFunds_AchFundsAvailability_Immediate_LimitExceeded() {
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);
    businessSettingsService.updateBusinessSettings(
        businessId,
        BusinessSettings.builder()
            .achFundsAvailabilityMode(AchFundsAvailabilityMode.IMMEDIATE)
            .immediateAchFundsLimit(BigDecimal.valueOf(500))
            .build());

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(1000));
    stripeMockClient.setClearspendFinancialAccountBalance(Amount.of(Currency.USD, 2000L));

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.DEPOSIT,
            amount,
            true);

    // standard lock, no available funds
    Account account =
        accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(adjustmentAndHoldRecord.hold().getExpirationDate())
        .isAfter(OffsetDateTime.now(Clock.systemUTC()).plusDays(4));
    assertThat(account.getAvailableBalance()).isEqualTo(Amount.of(Currency.USD, BigDecimal.ZERO));

    sendAchTransferCompletion(
        businessBankAccount.getId(),
        adjustmentAndHoldRecord.hold().getId(),
        adjustmentAndHoldRecord.adjustment().getId(),
        amount);

    // hold should be released and funds available
    account = accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.RELEASED);
    assertThat(account.getAvailableBalance())
        .isEqualTo(Amount.of(Currency.USD, BigDecimal.valueOf(1000)));
  }

  @Test
  void depositFunds_AchFundsAvailability_Immediate_NotEnoughFundsOnClearspendAccount() {
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    BusinessBankAccount businessBankAccount = testHelper.createBusinessBankAccount(businessId);
    businessSettingsService.updateBusinessSettings(
        businessId,
        BusinessSettings.builder()
            .achFundsAvailabilityMode(AchFundsAvailabilityMode.IMMEDIATE)
            .immediateAchFundsLimit(BigDecimal.valueOf(1000))
            .build());

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(1000));
    stripeMockClient.setClearspendFinancialAccountBalance(Amount.of(Currency.USD, 200L));

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.DEPOSIT,
            amount,
            true);

    // standard lock, no available funds
    Account account =
        accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(adjustmentAndHoldRecord.hold().getExpirationDate())
        .isAfter(OffsetDateTime.now(Clock.systemUTC()).plusDays(4));
    assertThat(account.getAvailableBalance()).isEqualTo(Amount.of(Currency.USD, BigDecimal.ZERO));

    sendAchTransferCompletion(
        businessBankAccount.getId(),
        adjustmentAndHoldRecord.hold().getId(),
        adjustmentAndHoldRecord.adjustment().getId(),
        amount);

    // hold should be released and funds available
    account = accountService.retrieveAccountById(adjustmentAndHoldRecord.account().getId(), true);
    assertThat(adjustmentAndHoldRecord.hold().getStatus()).isEqualTo(HoldStatus.RELEASED);
    assertThat(account.getAvailableBalance())
        .isEqualTo(Amount.of(Currency.USD, BigDecimal.valueOf(1000)));
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

    AccountActivity declinedActivity =
        accountActivityRepository.findAll().stream()
            .filter(a -> a.getStatus() == AccountActivityStatus.DECLINED)
            .findFirst()
            .orElseThrow();
    assertThat(declinedActivity.getDeclineDetails())
        .containsOnly(new DeclineDetails(DeclineReason.INSUFFICIENT_FUNDS));
  }

  @Test
  void transactBankAccount_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    final Amount amount = new Amount();
    amount.setCurrency(Currency.USD);
    amount.setAmount(new BigDecimal(10));
    final ThrowingSupplier<AdjustmentAndHoldRecord> action =
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
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
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
    final ThrowingSupplier<List<BusinessBankAccount>> action =
        () ->
            bankAccountService.getBusinessBankAccounts(
                createBusinessRecord.business().getId(), false);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
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
    final String bankName = "Bank";
    final String ownerName = "Owner";

    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount businessBankAccount =
        bankAccountService.createBusinessBankAccount(
            routingNumber,
            accountNumber,
            accountName,
            accessToken,
            accountRef,
            bankName,
            createBusinessRecord.business().getId(),
            AccountLinkStatus.LINKED);

    assertThat(businessBankAccount.getBusinessId())
        .isEqualTo(createBusinessRecord.business().getId());
    assertThat(businessBankAccount.getRoutingNumber())
        .isEqualTo(new EncryptedStringWithHash(routingNumber));
    assertThat(businessBankAccount.getAccountNumber())
        .isEqualTo(new EncryptedStringWithHash(accountNumber));
    assertThat(businessBankAccount.getAccessToken())
        .isEqualTo(new RequiredEncryptedStringWithHash(accessToken));
    assertThat(businessBankAccount.getPlaidAccountRef())
        .isEqualTo(new RequiredEncryptedStringWithHash(accountRef));
    assertThat(businessBankAccount.getName()).isEqualTo(accountName);
  }

  @Test
  void createBusinessBankAccount_UserPermissions() {
    final String routingNumber = "123";
    final String accountNumber = "456";
    final String accountName = "Account";
    final String accessToken = "Token";
    final String accountRef = "Ref";
    final String bankName = "Bank";
    final String ownerName = "Owner";
    final ThrowingSupplier<BusinessBankAccount> action =
        () ->
            bankAccountService.createBusinessBankAccount(
                routingNumber,
                accountNumber,
                accountName,
                accessToken,
                accountRef,
                bankName,
                createBusinessRecord.business().getId(),
                AccountLinkStatus.LINKED);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
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
    final ThrowingSupplier<String> action =
        () -> bankAccountService.getLinkToken(createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
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

    assertTrue(balance.getAvailable().isPositive());
    assertTrue(balance.getCurrent().isPositive());
    assertNull(balance.getLimit());
  }

  @Test
  void linkBusinessBankAccounts_UserPermissions() {
    final ThrowingSupplier<List<BusinessBankAccount>> action =
        () ->
            bankAccountService.linkBusinessBankAccounts(
                "link-token-mock-LinkToken", createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
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
                DeclineReason.ST_FAILED);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
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
                createBusinessRecord.business().getId(), bankAccount.getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
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
  void retrieveBusinessBankAccount_WhenBankNameIsEmpty() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount bankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    bankAccount.setBankName("");
    businessBankAccountRepository.saveAndFlush(bankAccount);
    assertTrue(StringUtils.isBlank(bankAccount.getBankName()));
    final BusinessBankAccount result =
        bankAccountService.retrieveBusinessBankAccount(bankAccount.getId());
    assertTrue(StringUtils.isNotBlank(result.getBankName()));
  }

  @Test
  void retrieveBusinessBankAccount_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccount bankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    final ThrowingSupplier<BusinessBankAccount> action =
        () -> bankAccountService.retrieveBusinessBankAccount(bankAccount.getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .build()
        .validateServiceMethod(action);
  }

  private void sendAchTransferCompletion(
      TypedId<BusinessBankAccountId> businessBankAccountId,
      TypedId<HoldId> holdId,
      TypedId<AdjustmentId> adjustmentId,
      Amount amount) {
    InboundTransfer inboundTransfer = new InboundTransfer();
    inboundTransfer.setMetadata(
        Map.of(
            StripeMetadataEntry.BUSINESS_ID.getKey(),
            createBusinessRecord.business().getId().toString(),
            StripeMetadataEntry.BUSINESS_BANK_ACCOUNT_ID.getKey(),
            businessBankAccountId.toString(),
            StripeMetadataEntry.HOLD_ID.getKey(),
            holdId.toString(),
            StripeMetadataEntry.ADJUSTMENT_ID.getKey(),
            adjustmentId.toString()));
    inboundTransfer.setCurrency("usd");
    inboundTransfer.setAmount(amount.toStripeAmount());

    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());

    stripeConnectHandlerAccessor.processInboundTransferResult(inboundTransfer);
  }
}
