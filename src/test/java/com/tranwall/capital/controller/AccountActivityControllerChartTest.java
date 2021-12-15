package com.tranwall.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.nonprod.TestDataController;
import com.tranwall.capital.controller.type.activity.ChartData;
import com.tranwall.capital.controller.type.activity.ChartDataRequest;
import com.tranwall.capital.controller.type.activity.ChartFilterType;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.NetworkMessageType;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessBankAccountService;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.UserService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
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

  private Bin bin;
  private Program program;

  @SneakyThrows
  @Test
  void getChartDataFilterTypeEmployee() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
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

    List<ChartData> chartData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(List.class, ChartData.class));
    assertEquals(5, chartData.size());
    log.info(response.getContentAsString());
  }

  private void createUserCardAndNetworkTransaction(
      CreateBusinessRecord createBusinessRecord, Business business, int transactions) {
    UserService.CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            program,
            Currency.USD);
    for (int i = 0; i < transactions; i++) {
      SecureRandom random = new SecureRandom();
      networkMessageService.processNetworkMessage(
          TestDataController.generateNetworkCommon(
              NetworkMessageType.FINANCIAL_TRANSACTION,
              user.user(),
              card,
              createBusinessRecord.allocationRecord().account(),
              program,
              Amount.of(Currency.USD, new BigDecimal(random.nextInt(100)))));
    }
  }

  @SneakyThrows
  @Test
  void getChartDataFilterTypeAllocation() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
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

    List<ChartData> chartData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(List.class, ChartData.class));
    assertEquals(5, chartData.size());
    log.info(response.getContentAsString());
  }

  private void createUserAllocationCardAndNetworkTransaction(
      CreateBusinessRecord createBusinessRecord,
      Business business,
      Account account,
      int transactions) {
    UserService.CreateUpdateUserRecord user = testHelper.createUser(business);
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
        testHelper.issueCard(business, allocation.allocation(), user.user(), program, Currency.USD);
    for (int i = 0; i < transactions; i++) {
      SecureRandom random = new SecureRandom();
      networkMessageService.processNetworkMessage(
          TestDataController.generateNetworkCommon(
              NetworkMessageType.FINANCIAL_TRANSACTION,
              user.user(),
              card,
              allocation.account(),
              program,
              Amount.of(Currency.USD, new BigDecimal(random.nextInt(100)))));
    }
  }

  @SneakyThrows
  @Test
  void getChartDataFilterTypeMerchant() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
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

    List<ChartData> chartData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(List.class, ChartData.class));
    assertEquals(1, chartData.size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getChartDataFilterTypeMerchantCategory() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
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

    List<ChartData> chartData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(List.class, ChartData.class));
    assertEquals(1, chartData.size());
    log.info(response.getContentAsString());
  }
}
