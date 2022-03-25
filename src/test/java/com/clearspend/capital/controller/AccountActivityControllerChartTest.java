package com.clearspend.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.type.activity.ChartDataRequest;
import com.clearspend.capital.controller.type.activity.ChartDataResponse;
import com.clearspend.capital.controller.type.activity.ChartFilterType;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.ServiceHelper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("JavaTimeDefaultTimeZone")
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class AccountActivityControllerChartTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final BusinessBankAccountService businessBankAccountService;
  private final AccountService accountService;
  private final NetworkMessageService networkMessageService;
  private final ServiceHelper serviceHelper;

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void getChartDataFilterTypeEmployee(boolean sortedDesc) {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("5000")),
        false);
    Account account =
        serviceHelper
            .accountService()
            .retrieveRootAllocationAccount(
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
    serviceHelper
        .accountService()
        .reallocateFunds(
            account.getId(),
            allocation.account().getId(),
            new Amount(Currency.USD, BigDecimal.valueOf(4000)));

    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 3);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 2);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 4);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);

    ChartDataRequest chartDataRequest = new ChartDataRequest();
    chartDataRequest.setChartFilter(ChartFilterType.EMPLOYEE);
    chartDataRequest.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
    chartDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    chartDataRequest.setTo(OffsetDateTime.now().plusDays(1));
    if (sortedDesc) {
      chartDataRequest.setDirection(Direction.DESC);
    }

    String body = objectMapper.writeValueAsString(chartDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/category-spend")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    ChartDataResponse chartData =
        objectMapper.readValue(response.getContentAsString(), ChartDataResponse.class);
    assertEquals(5, chartData.getUserChartData().size());
    assertEquals(
        chartData.getUserChartData().stream()
            .sorted(sortedDesc ? Comparator.reverseOrder() : Comparator.naturalOrder())
            .collect(Collectors.toList()),
        chartData.getUserChartData());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getChartDataFilterTypeAllocation() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("9000")),
        false);
    Account account =
        serviceHelper
            .accountService()
            .retrieveRootAllocationAccount(
                business.getId(),
                business.getCurrency(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                false);

    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, account, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, account, 4);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, account, 2);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, account, 5);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, account, 2);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, account, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, account, 1);

    Cookie authCookie = createBusinessRecord.authCookie();

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

  @SneakyThrows
  @Test
  void getChartDataFilterTypeMerchant() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("9000")),
        false);
    Account account =
        serviceHelper
            .accountService()
            .retrieveRootAllocationAccount(
                business.getId(),
                business.getCurrency(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                false);

    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 4);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 2);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 5);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 2);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);

    Cookie authCookie = createBusinessRecord.authCookie();

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
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("9000")),
        false);
    Account account =
        serviceHelper
            .accountService()
            .retrieveRootAllocationAccount(
                business.getId(),
                business.getCurrency(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                false);

    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 4);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 2);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 5);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 2);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);
    testHelper.createUserAllocationCardAndNetworkTransaction(
        createBusinessRecord, business, null, 1);

    Cookie authCookie = createBusinessRecord.authCookie();

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
