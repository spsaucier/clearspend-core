package com.clearspend.capital.controller.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestEnv;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.client.plaid.PlaidClientException;
import com.clearspend.capital.client.plaid.PlaidClientTest;
import com.clearspend.capital.client.plaid.PlaidProperties;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.business.BusinessBankAccountController.LinkTokenResponse;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.BankAccount;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.PendingStripeTransfer;
import com.clearspend.capital.data.model.business.AccountLinkStatus;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.PendingStripeTransferState;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.PendingStripeTransferService;
import com.clearspend.capital.service.ServiceHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javafaker.Faker;
import com.plaid.client.model.AccountBase;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.NoSuchElementException;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
  private final ServiceHelper serviceHelper;
  private final PlaidClient plaidClient;
  private final StripeClient stripeClient;

  private final PendingStripeTransferService pendingStripeTransferService;
  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;

  private Cookie authCookie;
  private CreateBusinessRecord createBusinessRecord;
  private BusinessBankAccount businessBankAccount;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.createBusiness(1000L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId(), false);
    this.authCookie = createBusinessRecord.authCookie();
  }

  @SneakyThrows
  @Test
  void linkToken_success() {
    assumeTrue(plaidProperties.isConfigured());
    createBusinessRecord.business().setOnboardingStep(BusinessOnboardingStep.LINK_ACCOUNT);
    testHelper.getLinkToken(testHelper.retrieveBusiness().getId());
  }

  @SneakyThrows
  @Test
  void linkedAccounts_success() {
    assumeTrue(plaidProperties.isConfigured());
    createBusinessRecord.business().setOnboardingStep(BusinessOnboardingStep.LINK_ACCOUNT);

    // Test a new linked account
    String linkToken = testHelper.getLinkToken(testHelper.retrieveBusiness().getId());
    usualGet(String.format("/business-bank-accounts/link-token/%s/accounts/", linkToken));
  }

  @SneakyThrows
  @Test
  void accounts_success() {
    getBusinessBankAccounts();
  }

  @NotNull
  private MockHttpServletResponse getBusinessBankAccounts() throws Exception {
    return usualGet("/business-bank-accounts");
  }

  private MockHttpServletResponse usualGet(String s) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get(s).contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    return response;
  }

  @SneakyThrows
  @Test
  void transact_success() {
    final BankAccountTransactType transactionType = BankAccountTransactType.DEPOSIT;
    final BigDecimal amount = BigDecimal.TEN;
    final TypedId<BusinessBankAccountId> businessBankAccountId = businessBankAccount.getId();
    final Cookie authCookie = this.authCookie;

    assertThat(transact(transactionType, amount, businessBankAccountId, authCookie).getStatus())
        .isEqualTo(200);
  }

  private MockHttpServletResponse transact(
      BankAccountTransactType transactionType,
      Number amountNum,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      Cookie authCookie)
      throws Exception {
    BigDecimal amount =
        amountNum instanceof BigDecimal
            ? (BigDecimal) amountNum
            : new BigDecimal(amountNum.toString());
    TransactBankAccountRequest request =
        new TransactBankAccountRequest(transactionType, new Amount(Currency.USD, amount));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format(
                        "/business-bank-accounts/%s/transactions", businessBankAccountId))
                    .contentType("application/json")
                    .cookie(authCookie)
                    .content(body))
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    return response;
  }

  @Test
  void transact_withoutFinancialAccount() {
    CreateBusinessRecord business = testHelper.createBusiness();
    testHelper.setCurrentUser(business.user());
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
        serviceHelper
            .pendingStripeTransferService()
            .retrievePendingTransfers(business.business().getId());
    assertThat(pendingStripeTransfers).hasSize(1);

    PendingStripeTransfer pendingStripeTransfer = pendingStripeTransfers.get(0);
    assertThat(pendingStripeTransfer.getAmount()).isEqualTo(amount.toAmount());
    assertThat(pendingStripeTransfer.getState()).isEqualTo(PendingStripeTransferState.PENDING);
  }

  @Test
  void transact_withFinancialAccount() {
    CreateBusinessRecord business = testHelper.createBusiness();
    testHelper.setCurrentUser(business.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(business.business().getId());

    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          businessService.updateBusinessStripeData(
              business.business().getId(),
              "stripeAccountRed",
              "stripeFinancialAccountRef",
              FinancialAccountState.READY,
              "stripeAccountNumber",
              "stripeRoutingNUmber");
        });

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
        serviceHelper
            .pendingStripeTransferService()
            .retrievePendingTransfers(business.business().getId());
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

    testHelper.setCurrentUser(createBusinessRecord.user());

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

  @Test
  @SneakyThrows
  void unregisterBankAccount() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    assertThat(businessBankAccount.getDeleted()).isFalse();

    mvc.perform(
            post(String.format(
                    "/business-bank-accounts/%s/unregister", businessBankAccount.getId()))
                .contentType("application/json")
                .cookie(authCookie))
        .andExpect(status().isOk())
        .andReturn();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccount =
        businessBankAccountService.retrieveBusinessBankAccount(businessBankAccount.getId());

    assertThat(businessBankAccount.getDeleted()).isTrue();
  }

  @SneakyThrows
  @Test
  @DisabledIfEnvironmentVariable(
      named = TestEnv.FAST_TEST_EXECUTION,
      matches = "true",
      disabledReason = "To speed up test execution")
  void plaidPasswordReset() {
    assumeTrue(plaidClient.isConfigured());
    // not using the regular one for this test
    final TypedId<BusinessId> businessId = PlaidClientTest.businessId();
    createBusinessRecord = testHelper.createBusiness(businessId);
    authCookie = createBusinessRecord.authCookie();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount linkedAccount =
        businessBankAccount =
            testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    final String accessToken = linkedAccount.getAccessToken().getEncrypted();
    List<AccountBase> balances = plaidClient.getBalances(businessId, accessToken);

    assertNotNull(balances);

    assertThat(getBusinessBankAccountStatus(linkedAccount.getId()))
        .isEqualTo(AccountLinkStatus.LINKED);
    MockHttpServletResponse response = invalidateLink(createBusinessRecord, linkedAccount);

    // Here we check that an exception is thrown from the client, but the
    // client doesn't actually flag the account - that responsibility belongs to the service
    assertThatThrownBy(() -> plaidClient.getBalances(businessId, accessToken))
        .isInstanceOf(PlaidClientException.class);

    // Trigger the account status change
    MockHttpServletResponse fundAccountResponse =
        transact(
            BankAccountTransactType.DEPOSIT,
            10,
            linkedAccount.getId(),
            createBusinessRecord.authCookie());
    assertThat(fundAccountResponse.getStatus()).isEqualTo(428);

    assertThat(getBusinessBankAccountStatus(linkedAccount.getId()))
        .isEqualTo(AccountLinkStatus.RE_LINK_REQUIRED);

    // Re-check for good measure
    fundAccountResponse =
        transact(
            BankAccountTransactType.DEPOSIT,
            10,
            linkedAccount.getId(),
            createBusinessRecord.authCookie());
    assertThat(fundAccountResponse.getStatus()).isEqualTo(428);

    MockHttpServletResponse response2 =
        mvc.perform(
                get(
                        "/business-bank-accounts/re-link/{businessBankAccountId}",
                        linkedAccount.getId().toString())
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .cookie(createBusinessRecord.authCookie())
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertNotNull(response2);
    LinkTokenResponse linkTokenResponse =
        objectMapper.readValue(response2.getContentAsString(), LinkTokenResponse.class);
    assertTrue(linkTokenResponse.linkToken().startsWith("link-sandbox-"));
  }

  @SneakyThrows
  private AccountLinkStatus getBusinessBankAccountStatus(TypedId<BusinessBankAccountId> id) {
    final List<BankAccount> bankAccounts =
        objectMapper.readValue(
            getBusinessBankAccounts().getContentAsString(),
            new TypeReference<List<BankAccount>>() {});
    if (bankAccounts.isEmpty()) {
      throw new NoSuchElementException("No bank accounts");
    }
    return bankAccounts.stream()
        .filter(ba -> ba.getBusinessBankAccountId().equals(id))
        .findFirst()
        .map(BankAccount::getAccountLinkStatus)
        .orElseThrow(() -> new NoSuchElementException("No matching account"));
  }

  @NotNull
  private MockHttpServletResponse invalidateLink(
      CreateBusinessRecord createBusinessRecord, BusinessBankAccount linkedAccount)
      throws Exception {
    return mvc.perform(
            get(
                    "/non-production/test-data/plaid/un-link/{businessBankAccountId}",
                    linkedAccount.getId().toString())
                .header(
                    HttpHeaders.USER_AGENT,
                    new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                .cookie(createBusinessRecord.authCookie())
                .contentType("application/json"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }
}
