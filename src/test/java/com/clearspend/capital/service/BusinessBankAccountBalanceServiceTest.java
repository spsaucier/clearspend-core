package com.clearspend.capital.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.client.plaid.TestPlaidClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.BalanceNotFoundException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.business.BusinessBankAccountBalanceRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BusinessBankAccountBalanceServiceTest extends BaseCapitalTest {
  private static final double CURRENT = 100;
  private static final double AVAILABLE = 90;
  private static final double LIMIT = 50;
  private static final Amount CURRENT_AMOUNT = toAmount(CURRENT);
  private static final Amount AVAILABLE_AMOUNT = toAmount(AVAILABLE);
  private static final Amount LIMIT_AMOUNT = toAmount(LIMIT);
  private static final String ACCESS_TOKEN = "access-mock-Token";
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

  private static Amount toAmount(final double value) {
    return new Amount(Currency.USD, BigDecimal.valueOf(value));
  }

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessBankAccountBalanceService businessBankAccountBalanceService;
  @Autowired private BusinessBankAccountBalanceRepository businessBankAccountBalanceRepo;
  @Autowired private BusinessBankAccountRepository businessBankAccountRepo;
  @Autowired private PlaidClient plaidClient;
  @Autowired private PermissionValidationHelper permissionValidationHelper;
  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private BusinessBankAccount businessBankAccount;

  @BeforeEach
  public void setup() throws Exception {
    assumeTrue(plaidClient.isConfigured());
    final Map.Entry<TypedId<BusinessId>, String> businessSandboxEntry =
        TestPlaidClient.INSTITUTION_SANDBOX_ID_BY_BUSINESS_ID.entrySet().stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Unable to get TestPlaidClient business"));
    createBusinessRecord = testHelper.createBusiness(businessSandboxEntry.getKey());

    final AccountBase accountBase =
        plaidClient.getBalances(ACCESS_TOKEN, createBusinessRecord.business().getId()).stream()
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException("Unable to get TestPlaidClient balance record"));

    businessBankAccount =
        businessBankAccountRepo.save(
            createAccount(createBusinessRecord.business().getId(), accountBase.getAccountId()));
  }

  private BusinessBankAccount createAccount(
      final TypedId<BusinessId> businessId, final String plaidAccountRef) {
    final BusinessBankAccount account = new BusinessBankAccount();
    account.setId(new TypedId<>(UUID.randomUUID()));
    account.setBusinessId(businessId);
    account.setName(String.format("TheBusiness-%s", UUID.randomUUID()));
    account.setRoutingNumber(new RequiredEncryptedStringWithHash("Routing"));
    account.setAccountNumber(new RequiredEncryptedStringWithHash("Account"));
    account.setAccessToken(new RequiredEncryptedStringWithHash(ACCESS_TOKEN));
    account.setPlaidAccountRef(new RequiredEncryptedStringWithHash(plaidAccountRef));
    account.setDeleted(false);
    return account;
  }

  @Test
  void createBusinessBankAccountBalance() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccountBalance createdBalance =
        businessBankAccountBalanceService.createBusinessBankAccountBalance(
            businessBankAccount, CURRENT_AMOUNT, AVAILABLE_AMOUNT, LIMIT_AMOUNT);
    validateBalance(createdBalance);

    final Optional<BusinessBankAccountBalance> dbBalance =
        businessBankAccountBalanceRepo.findById(createdBalance.getId());
    assertTrue(dbBalance.isPresent());
    validateBalance(dbBalance.get());
  }

  private void validateBalance(final BusinessBankAccountBalance balance) {
    assertThat(
        balance,
        allOf(
            hasProperty("businessBankAccountId", equalTo(businessBankAccount.getId())),
            hasProperty("available", equalTo(AVAILABLE_AMOUNT)),
            hasProperty("current", equalTo(CURRENT_AMOUNT)),
            hasProperty("limit", equalTo(LIMIT_AMOUNT))));
  }

  @Test
  void createBusinessBankAccountBalance_UserPermissions() {
    final ThrowingRunnable action =
        () ->
            businessBankAccountBalanceService.createBusinessBankAccountBalance(
                businessBankAccount, CURRENT_AMOUNT, AVAILABLE_AMOUNT, LIMIT_AMOUNT);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void createBusinessBankAccountBalance_WithAccountBalance() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final AccountBalance balance = new AccountBalance();
    balance.setIsoCurrencyCode("USD");
    balance.setCurrent(CURRENT);
    balance.setAvailable(AVAILABLE);
    balance.setLimit(LIMIT);
    final BusinessBankAccountBalance createdBalance =
        businessBankAccountBalanceService.createBusinessBankAccountBalance(
            businessBankAccount, balance);
    validateBalance(createdBalance);

    final Optional<BusinessBankAccountBalance> dbBalance =
        businessBankAccountBalanceRepo.findById(createdBalance.getId());
    assertTrue(dbBalance.isPresent());
    validateBalance(dbBalance.get());
  }

  @Test
  void createBusinessBankAccountBalance_WithAccountBalance_UserPermissions() {
    final AccountBalance balance = new AccountBalance();
    balance.setIsoCurrencyCode("USD");
    balance.setCurrent(CURRENT);
    balance.setAvailable(AVAILABLE);
    balance.setLimit(LIMIT);
    final ThrowingRunnable action =
        () ->
            businessBankAccountBalanceService.createBusinessBankAccountBalance(
                businessBankAccount, balance);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getNewBalance() throws IOException {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final BusinessBankAccountBalance initialBalance = new BusinessBankAccountBalance();
    initialBalance.setBusinessBankAccountId(businessBankAccount.getId());
    initialBalance.setCurrent(CURRENT_AMOUNT);
    initialBalance.setAvailable(AVAILABLE_AMOUNT);
    initialBalance.setLimit(LIMIT_AMOUNT);
    businessBankAccountBalanceRepo.save(initialBalance);
    final BusinessBankAccountBalance createdBalance =
        businessBankAccountBalanceService.getNewBalance(businessBankAccount);
    assertThat(
        createdBalance,
        allOf(
            hasProperty("businessBankAccountId", equalTo(businessBankAccount.getId())),
            hasProperty("available", hasProperty("amount", equalTo(new BigDecimal("10000.0")))),
            hasProperty("current", hasProperty("amount", equalTo(new BigDecimal("10100.0")))),
            hasProperty("limit", nullValue())));
  }

  @Test
  void getNewBalance_noBalanceFound() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final TestHelper.CreateBusinessRecord business = testHelper.createBusiness();
    testHelper.setCurrentUser(business.user());
    final BusinessBankAccount bankAccount =
        businessBankAccountRepo.save(createAccount(business.business().getId(), "ABCDEFG"));
    assertThrows(
        BalanceNotFoundException.class,
        () -> businessBankAccountBalanceService.getNewBalance(bankAccount));
  }

  @Test
  void getNewBalance_UserPermissions() {
    final ThrowingRunnable action =
        () -> businessBankAccountBalanceService.getNewBalance(businessBankAccount);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(rootAllocationFailingRoles)
        .addAllChildAllocationFailingRoles(childAllocationFailingRoles)
        .useDefaultChildAllocation()
        .build()
        .validateServiceMethod(action);
  }
}
