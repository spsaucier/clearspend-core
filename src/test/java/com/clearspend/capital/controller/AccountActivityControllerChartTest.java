package com.clearspend.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.type.activity.ChartDataRequest;
import com.clearspend.capital.controller.type.activity.ChartDataResponse;
import com.clearspend.capital.controller.type.activity.ChartFilterType;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessBankAccount;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class AccountActivityControllerChartTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final BusinessBankAccountService businessBankAccountService;
  private final AccountService accountService;
  private final NetworkMessageService networkMessageService;

  @SneakyThrows
  @Test
  void getChartDataFilterTypeEmployee() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("5000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            testHelper.createUser(business).user());
    accountService.reallocateFunds(
        account.getId(),
        allocation.account().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(4000)));

    createUserCardAndNetworkTransaction(createBusinessRecord, business, 1);
    createUserCardAndNetworkTransaction(createBusinessRecord, business, 3);
    createUserCardAndNetworkTransaction(createBusinessRecord, business, 2);
    createUserCardAndNetworkTransaction(createBusinessRecord, business, 1);
    createUserCardAndNetworkTransaction(createBusinessRecord, business, 4);
    createUserCardAndNetworkTransaction(createBusinessRecord, business, 1);

    ChartDataRequest chartDataRequest = new ChartDataRequest();
    chartDataRequest.setChartFilter(ChartFilterType.EMPLOYEE);
    chartDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    chartDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(chartDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/category-spend")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ChartDataResponse chartData =
        objectMapper.readValue(response.getContentAsString(), ChartDataResponse.class);
    assertEquals(5, chartData.getUserChartData().size());
    log.info(response.getContentAsString());
  }

  private void createUserCardAndNetworkTransaction(
      CreateBusinessRecord createBusinessRecord, Business business, int transactions)
      throws JsonProcessingException {
    CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL);
    SecureRandom random = new SecureRandom();
    for (int i = 0; i < transactions; i++) {
      networkMessageService.processNetworkMessage(
          TestDataController.generateNetworkCommon(
              NetworkMessageType.AUTH_REQUEST,
              user.user(),
              card,
              createBusinessRecord.allocationRecord().account(),
              Amount.of(Currency.USD, new BigDecimal(random.nextInt(99)).add(BigDecimal.ONE))));
    }
  }

  @SneakyThrows
  @Test
  void getChartDataFilterTypeAllocation() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("9000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);

    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 4);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 2);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 5);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 2);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);

    ChartDataRequest chartDataRequest = new ChartDataRequest();
    chartDataRequest.setChartFilter(ChartFilterType.ALLOCATION);
    chartDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    chartDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(chartDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/category-spend")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ChartDataResponse chartData =
        objectMapper.readValue(response.getContentAsString(), ChartDataResponse.class);
    assertEquals(5, chartData.getAllocationChartData().size());
    log.info(response.getContentAsString());
  }

  private void createUserAllocationCardAndNetworkTransaction(
      CreateBusinessRecord createBusinessRecord,
      Business business,
      Account account,
      int transactions)
      throws JsonProcessingException {
    CreateUpdateUserRecord user = testHelper.createUser(business);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            testHelper.generateAccountName(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            user.user());
    accountService.reallocateFunds(
        account.getId(),
        allocation.account().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(1000)));
    Card card =
        testHelper.issueCard(
            business,
            allocation.allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL);
    SecureRandom random = new SecureRandom();
    for (int i = 0; i < transactions; i++) {
      networkMessageService.processNetworkMessage(
          TestDataController.generateNetworkCommon(
              NetworkMessageType.AUTH_REQUEST,
              user.user(),
              card,
              allocation.account(),
              Amount.of(Currency.USD, new BigDecimal(random.nextInt(99)).add(BigDecimal.ONE))));
    }
  }

  @SneakyThrows
  @Test
  void getChartDataFilterTypeMerchant() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("9000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);

    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 4);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 2);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 5);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 2);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);

    ChartDataRequest chartDataRequest = new ChartDataRequest();
    chartDataRequest.setChartFilter(ChartFilterType.MERCHANT);
    chartDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    chartDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(chartDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/category-spend")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ChartDataResponse chartData =
        objectMapper.readValue(response.getContentAsString(), ChartDataResponse.class);
    assertEquals(5, chartData.getMerchantChartData().size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getChartDataFilterTypeMerchantCategory() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("9000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);

    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 4);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 2);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 5);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 2);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);
    createUserAllocationCardAndNetworkTransaction(createBusinessRecord, business, account, 1);

    ChartDataRequest chartDataRequest = new ChartDataRequest();
    chartDataRequest.setChartFilter(ChartFilterType.MERCHANT_CATEGORY);
    chartDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    chartDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(chartDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/category-spend")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ChartDataResponse chartData =
        objectMapper.readValue(response.getContentAsString(), ChartDataResponse.class);
    assertEquals(1, chartData.getMerchantCategoryChartData().size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getChartDataFilterWhenNoTransactionsArePresent() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    ChartDataRequest chartDataRequest = new ChartDataRequest();
    chartDataRequest.setChartFilter(ChartFilterType.EMPLOYEE);
    chartDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    chartDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(chartDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/category-spend")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ChartDataResponse chartData =
        objectMapper.readValue(response.getContentAsString(), ChartDataResponse.class);
    assertNull(chartData.getMerchantChartData());
    assertEquals(0, chartData.getUserChartData().size());
    assertNull(chartData.getAllocationChartData());
    assertNull(chartData.getMerchantCategoryChartData());
    log.info(response.getContentAsString());
  }
}
