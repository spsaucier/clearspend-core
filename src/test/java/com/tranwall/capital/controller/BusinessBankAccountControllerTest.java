package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.client.plaid.PlaidProperties;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import java.math.BigDecimal;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class BusinessBankAccountControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final PlaidProperties plaidProperties;

  private Cookie authCookie;

  @BeforeEach
  void init() {
    testHelper.init();
    this.authCookie = testHelper.login("tester@tranwall.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void linkToken_success() {
    if (StringUtils.isBlank(plaidProperties.getSecret())) {
      return;
    }
    testHelper.getLinkToken(testHelper.retrieveBusiness().getId());
  }

  @SneakyThrows
  @Test
  void linkedAccounts_success() {
    if (StringUtils.isBlank(plaidProperties.getSecret())) {
      log.warn("skipping test due to missing Plaid credentials");
      return;
    }
    String linkToken = testHelper.getLinkToken(testHelper.retrieveBusiness().getId());
    MockHttpServletResponse response =
        mvc.perform(
                get(String.format("/business-bank-accounts/link-token/%s/accounts/", linkToken))
                    .header("businessId", testHelper.retrieveBusiness().getId().toString())
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void accounts_success() {
    if (StringUtils.isBlank(plaidProperties.getSecret())) {
      return;
    }
    MockHttpServletResponse response =
        mvc.perform(
                get("/business-bank-accounts")
                    .header("businessId", testHelper.retrieveBusiness().getId().toString())
                    .contentType("application/json")
                    .cookie(authCookie))
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
            FundsTransactType.DEPOSIT, new Amount(Currency.USD, BigDecimal.TEN));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format(
                        "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                    .header("businessId", testHelper.retrieveBusiness().getId().toString())
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }
}
