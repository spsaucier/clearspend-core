package com.clearspend.capital.service;

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
import com.clearspend.capital.common.typedid.data.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.BusinessBankAccount;
import com.clearspend.capital.data.model.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.BusinessBankAccountBalanceRepository;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
class BusinessBankAccountServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessBankAccountService bankAccountService;
  @Autowired private PlaidClient plaidClient;
  @Autowired private BusinessBankAccountBalanceRepository businessBankAccountBalanceRepository;

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
  void depositFunds() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccountId,
            BankAccountTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            true);
  }

  @Test
  void withdrawFunds_success() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("700.51")),
        false);
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        bankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccountId,
            BankAccountTransactType.WITHDRAW,
            Amount.of(Currency.USD, new BigDecimal("241.85")),
            true);
  }

  @Test
  void withdrawFunds_insufficientBalance() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessId> businessId = createBusinessRecord.business().getId();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("710.51")),
        true);

    InsufficientFundsException insufficientFundsException =
        assertThrows(
            InsufficientFundsException.class,
            () ->
                bankAccountService.transactBankAccount(
                    createBusinessRecord.business().getId(),
                    businessBankAccountId,
                    BankAccountTransactType.WITHDRAW,
                    Amount.of(Currency.USD, new BigDecimal("1.85")),
                    true));
  }

  @Test
  void depositFunds_insufficientFunds() {
    assumeTrue(plaidClient.isConfigured());
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    InsufficientFundsException insufficientFundsException =
        assertThrows(
            InsufficientFundsException.class,
            () ->
                bankAccountService.transactBankAccount(
                    createBusinessRecord.business().getId(),
                    businessBankAccountId,
                    BankAccountTransactType.DEPOSIT,
                    Amount.of(Currency.USD, new BigDecimal("15000.00")),
                    true));
  }

  @SneakyThrows
  @Test
  void testLinkBusinessBankAccounts() {
    assumeTrue(plaidClient.isConfigured());
    int balancesBeforeTest = businessBankAccountBalanceRepository.findAll().size();

    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
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
}
