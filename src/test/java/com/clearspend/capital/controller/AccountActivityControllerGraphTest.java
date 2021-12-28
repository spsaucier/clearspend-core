package com.clearspend.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.type.activity.DashboardGraphData;
import com.clearspend.capital.controller.type.activity.GraphDataRequest;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessReallocationType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import java.math.BigDecimal;
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
public class AccountActivityControllerGraphTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessService businessService;
  private final AccountService accountService;
  private final NetworkMessageService networkMessageService;

  private Bin bin;
  private Program program;

  @SneakyThrows
  @Test
  void getGraphData() {
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
        Amount.of(Currency.USD, new BigDecimal("1000")),
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
        new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        allocation.allocation().getId(),
        allocation.account().getId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            program,
            Currency.USD);

    networkMessageService.processNetworkMessage(
        TestDataController.generateNetworkCommon(
            NetworkMessageType.PRE_AUTH,
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            program,
            Amount.of(Currency.USD, BigDecimal.valueOf(100))));

    networkMessageService.processNetworkMessage(
        TestDataController.generateNetworkCommon(
            NetworkMessageType.FINANCIAL_AUTH,
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            program,
            Amount.of(Currency.USD, BigDecimal.valueOf(200))));

    GraphDataRequest graphDataRequest = new GraphDataRequest();
    graphDataRequest.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
    graphDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    graphDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(graphDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/graph-data")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    DashboardGraphData dashboardGraphData =
        objectMapper.readValue(response.getContentAsString(), DashboardGraphData.class);
    assertEquals(2, dashboardGraphData.getGraphData().size());
    assertEquals(BigDecimal.valueOf(30000, 2), dashboardGraphData.getTotalSpend());
    assertEquals(BigDecimal.valueOf(15000, 2), dashboardGraphData.getAverageSpend());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getGraphDataWhenNoTransactionsArePresent() {
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

    GraphDataRequest graphDataRequest = new GraphDataRequest();
    graphDataRequest.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
    graphDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    graphDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(graphDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/graph-data")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    DashboardGraphData dashboardGraphData =
        objectMapper.readValue(response.getContentAsString(), DashboardGraphData.class);
    assertEquals(0, dashboardGraphData.getGraphData().size());
    assertEquals(BigDecimal.valueOf(0), dashboardGraphData.getTotalSpend());
    assertEquals(BigDecimal.valueOf(0), dashboardGraphData.getAverageSpend());
    log.info(response.getContentAsString());
  }
}
