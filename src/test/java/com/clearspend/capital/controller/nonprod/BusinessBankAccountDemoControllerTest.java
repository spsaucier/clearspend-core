package com.clearspend.capital.controller.nonprod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestEnv;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@DisabledIfEnvironmentVariable(
    named = TestEnv.FAST_TEST_EXECUTION,
    matches = "true",
    disabledReason = "To speed up test execution")
class BusinessBankAccountDemoControllerTest extends BaseCapitalTest {

  @Autowired private final MockMvc mvc;
  @Autowired TestHelper testHelper;

  @SneakyThrows
  @Test
  void transactTest() {

    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    TransactBankAccountRequest transactBankAccountRequest =
        new TransactBankAccountRequest(
            BankAccountTransactType.DEPOSIT, new Amount(Currency.USD, BigDecimal.TEN));

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format(
                        "/non-production/business-bank-accounts/%s/transactions",
                        businessBankAccount.getId()))
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(transactBankAccountRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateAdjustmentResponse createAdjustmentResponse =
        objectMapper.readValue(response.getContentAsString(), CreateAdjustmentResponse.class);

    Assertions.assertNotNull(createAdjustmentResponse.getAdjustmentId());
  }
}
