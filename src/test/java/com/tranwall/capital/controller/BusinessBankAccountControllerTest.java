package com.tranwall.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.client.plaid.PlaidProperties;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.service.BusinessBankAccountService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
class BusinessBankAccountControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final PlaidProperties plaidProperties;

  private Cookie authCookie;

  @BeforeEach
  void init() {
    testHelper.init();
    this.authCookie = testHelper.login("business-owner-tester@clearspend.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void linkToken_success() {
    assumeTrue(plaidProperties.isConfigured());
    testHelper.getLinkToken(testHelper.retrieveBusiness().getId());
  }

  @SneakyThrows
  @Test
  void linkedAccounts_success() {
    assumeTrue(plaidProperties.isConfigured());

    // Set up to capture the log
    Logger accountLogger = (Logger) LoggerFactory.getLogger(BusinessBankAccountService.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    accountLogger.addAppender(listAppender);

    // Test a new linked account
    String linkToken = testHelper.getLinkToken(testHelper.retrieveBusiness().getId());
    MockHttpServletResponse response =
        mvc.perform(
                get(String.format("/business-bank-accounts/link-token/%s/accounts/", linkToken))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());

    // Since we didn't mock the user in line with the Plaid sandbox, there should be a
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
  }

  @SneakyThrows
  @Test
  void accounts_success() {
    assumeTrue(plaidProperties.isConfigured());
    MockHttpServletResponse response =
        mvc.perform(
                get("/business-bank-accounts").contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void transact_success() {
    BusinessBankAccount businessBankAccount = testHelper.retrieveBusinessBankAccount();

    TransactBankAccountRequest request =
        new TransactBankAccountRequest(
            BankAccountTransactType.DEPOSIT, new Amount(Currency.USD, BigDecimal.TEN));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format(
                        "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                    .contentType("application/json")
                    .cookie(authCookie)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }
}
