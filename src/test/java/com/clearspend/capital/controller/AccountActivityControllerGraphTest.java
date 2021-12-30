package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessReallocationType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
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

  @SneakyThrows
  @Test
  void getGraphData() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

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
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL);

    NetworkMessage networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                NetworkMessageType.PRE_AUTH,
                user.user(),
                card,
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(10))));
    assertThat(networkMessage.getHoldId()).isNotNull();

    networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                NetworkMessageType.PRE_AUTH,
                user.user(),
                card,
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(10))));
    assertThat(networkMessage.getHoldId()).isNotNull();

    networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                NetworkMessageType.FINANCIAL_AUTH,
                user.user(),
                card,
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(2))));
    assertThat(networkMessage.getAdjustmentId()).isNotNull();

    networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                NetworkMessageType.FINANCIAL_AUTH,
                user.user(),
                card,
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(4))));
    assertThat(networkMessage.getAdjustmentId()).isNotNull();

    networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                NetworkMessageType.FINANCIAL_AUTH,
                user.user(),
                card,
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(8))));
    assertThat(networkMessage.getAdjustmentId()).isNotNull();

    networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                NetworkMessageType.FINANCIAL_AUTH,
                user.user(),
                card,
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(20))));
    assertThat(networkMessage.getAdjustmentId()).isNotNull();

    networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                NetworkMessageType.FINANCIAL_AUTH,
                user.user(),
                card,
                createBusinessRecord.allocationRecord().account(),
                Amount.of(Currency.USD, BigDecimal.valueOf(9))));
    assertThat(networkMessage.getAdjustmentId()).isNotNull();

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
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    DashboardGraphData dashboardGraphData =
        objectMapper.readValue(response.getContentAsString(), DashboardGraphData.class);
    assertEquals(7, dashboardGraphData.getGraphData().size());
    assertEquals(
        BigDecimal.valueOf(9 + 20 + 8 + 4 + 2 + 10 + 10, 0).negate(),
        dashboardGraphData.getTotalSpend().setScale(0, RoundingMode.DOWN));
    assertEquals(
        dashboardGraphData.getTotalSpend().divide(new BigDecimal(7), 2, RoundingMode.DOWN),
        dashboardGraphData.getAverageSpend());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getGraphDataWhenNoTransactionsArePresent() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

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
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    DashboardGraphData dashboardGraphData =
        objectMapper.readValue(response.getContentAsString(), DashboardGraphData.class);
    assertEquals(7, dashboardGraphData.getGraphData().size());
    assertEquals(
        BigDecimal.valueOf(0).setScale(2, RoundingMode.DOWN),
        dashboardGraphData.getTotalSpend().setScale(2, RoundingMode.DOWN));
    assertEquals(
        BigDecimal.valueOf(0).setScale(2, RoundingMode.DOWN),
        dashboardGraphData.getAverageSpend().setScale(2, RoundingMode.DOWN));
    log.info(response.getContentAsString());
  }
}
