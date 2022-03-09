package com.clearspend.capital.controller.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.plaid.PlaidProperties;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.PendingStripeTransfer;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.PendingStripeTransferState;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.PendingStripeTransferService;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class BusinessBankAccountControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mvcHelper;
  private final TestHelper testHelper;
  private final PlaidProperties plaidProperties;

  private final PendingStripeTransferService pendingStripeTransferService;
  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;

  private Cookie authCookie;
  private CreateBusinessRecord createBusinessRecord;
  private BusinessBankAccount businessBankAccount;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.createBusiness(1000L);
    businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    this.authCookie = createBusinessRecord.authCookie();
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

  @Test
  void transact_withoutFinancialAccount() {
    CreateBusinessRecord business = testHelper.createBusiness();
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(business.business().getId());

    assertThat(business.business().getStripeData().getFinancialAccountState())
        .isEqualTo(FinancialAccountState.NOT_READY);

    Amount amount = new Amount(Currency.USD, new BigDecimal(9223));
    mvcHelper.queryObject(
        "/business-bank-accounts/%s/transactions".formatted(businessBankAccount.getId()),
        HttpMethod.POST,
        business.authCookie(),
        new TransactBankAccountRequest(BankAccountTransactType.DEPOSIT, amount),
        CreateAdjustmentResponse.class);

    List<PendingStripeTransfer> pendingStripeTransfers =
        pendingStripeTransferService.retrievePendingTransfers(business.business().getId());
    assertThat(pendingStripeTransfers).hasSize(1);

    PendingStripeTransfer pendingStripeTransfer = pendingStripeTransfers.get(0);
    assertThat(pendingStripeTransfer.getAmount()).isEqualTo(amount.toAmount());
    assertThat(pendingStripeTransfer.getState()).isEqualTo(PendingStripeTransferState.PENDING);
  }

  @Test
  void transact_withFinancialAccount() {
    CreateBusinessRecord business = testHelper.createBusiness();
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(business.business().getId());

    businessService.updateBusinessStripeData(
        business.business().getId(),
        "stripeAccountRed",
        "stripeFinancialAccountRef",
        FinancialAccountState.READY,
        "stripeAccountNumber",
        "stripeRoutingNUmber");

    assertThat(business.business().getStripeData().getFinancialAccountState())
        .isEqualTo(FinancialAccountState.READY);

    Amount amount = new Amount(Currency.USD, new BigDecimal(9223));
    mvcHelper.queryObject(
        "/business-bank-accounts/%s/transactions".formatted(businessBankAccount.getId()),
        HttpMethod.POST,
        business.authCookie(),
        new TransactBankAccountRequest(BankAccountTransactType.DEPOSIT, amount),
        CreateAdjustmentResponse.class);

    List<PendingStripeTransfer> pendingStripeTransfers =
        pendingStripeTransferService.retrievePendingTransfers(business.business().getId());
    assertThat(pendingStripeTransfers).hasSize(0);
  }

  @Test
  @SneakyThrows
  void registerBankAccount() {
    assertThat(businessBankAccount.getStripeBankAccountRef()).isNull();
    assertThat(businessBankAccount.getStripeSetupIntentRef()).isNull();

    mvc.perform(
            post(String.format("/business-bank-accounts/%s/register", businessBankAccount.getId()))
                .contentType("application/json")
                .cookie(authCookie))
        .andExpect(status().isOk())
        .andReturn();

    businessBankAccount =
        businessBankAccountService.retrieveBusinessBankAccount(businessBankAccount.getId());
    assertThat(businessBankAccount.getStripeBankAccountRef()).isNotNull();
    assertThat(businessBankAccount.getStripeSetupIntentRef()).isNotNull();
  }

  @Test
  @SneakyThrows
  void transact_withinAchDepositOperationLimit() {
    TransactBankAccountRequest request =
        new TransactBankAccountRequest(
            BankAccountTransactType.DEPOSIT, new Amount(Currency.USD, BigDecimal.TEN));

    String body = objectMapper.writeValueAsString(request);
    // default operation amount is 2 initial balance gets one operation so we can only perform 1
    // more
    mvc.perform(
            post(String.format(
                    "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                .contentType("application/json")
                .cookie(authCookie)
                .content(body))
        .andExpect(status().isOk())
        .andReturn();

    mvc.perform(
            post(String.format(
                    "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                .contentType("application/json")
                .cookie(authCookie)
                .content(body))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @SneakyThrows
  void transact_withinAchWithdrawOperationLimit() {
    TransactBankAccountRequest request =
        new TransactBankAccountRequest(
            BankAccountTransactType.WITHDRAW, new Amount(Currency.USD, BigDecimal.TEN));

    String body = objectMapper.writeValueAsString(request);
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post(String.format(
                      "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                  .contentType("application/json")
                  .cookie(authCookie)
                  .content(body))
          .andExpect(status().isOk())
          .andReturn();
    }

    mvc.perform(
            post(String.format(
                    "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                .contentType("application/json")
                .cookie(authCookie)
                .content(body))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @SneakyThrows
  void transact_withinAchDepositLimit() {
    TransactBankAccountRequest request =
        new TransactBankAccountRequest(
            BankAccountTransactType.DEPOSIT, new Amount(Currency.USD, new BigDecimal(10_001)));

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            post(String.format(
                    "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                .contentType("application/json")
                .cookie(authCookie)
                .content(body))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @SneakyThrows
  void transact_withinAchWithdrawLimit() {
    TransactBankAccountRequest request =
        new TransactBankAccountRequest(
            BankAccountTransactType.WITHDRAW, new Amount(Currency.USD, new BigDecimal(10_001)));

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            post(String.format(
                    "/business-bank-accounts/%s/transactions", businessBankAccount.getId()))
                .contentType("application/json")
                .cookie(authCookie)
                .content(body))
        .andExpect(status().isBadRequest())
        .andReturn();
  }
}
