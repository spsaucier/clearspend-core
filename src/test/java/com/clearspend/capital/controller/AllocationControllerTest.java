package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.allocation.AllocationDetailsResponse;
import com.clearspend.capital.controller.type.allocation.AllocationFundCardRequest;
import com.clearspend.capital.controller.type.allocation.CreateAllocationRequest;
import com.clearspend.capital.controller.type.allocation.UpdateAllocationRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.card.limits.Limit;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.MccGroup;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionChannel;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.MccGroupService;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
class AllocationControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final MccGroupService mccGroupService;

  private final Faker faker = new Faker();

  @BeforeEach
  void init() {
    testHelper.init();
  }

  @SneakyThrows
  @Test
  void createAllocation_success() {
    final Allocation rootAllocation =
        allocationService.getRootAllocation(testHelper.retrieveBusiness().getId()).allocation();
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            testHelper.generateFullName(),
            rootAllocation.getId(),
            testHelper.createUser(testHelper.retrieveBusiness()).user().getId(),
            new Amount(Currency.USD, BigDecimal.ZERO),
            Collections.singletonList(new CurrencyLimit(Currency.USD, new HashMap<>())),
            Collections.emptyList(),
            Collections.emptySet());

    String body = objectMapper.writeValueAsString(request);

    Cookie authCookie = testHelper.login(rootAllocation.getOwnerId());

    MockHttpServletResponse response =
        mvc.perform(
                post("/allocations")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getAllocation_success() {
    Business business = testHelper.retrieveBusiness();
    final Allocation rootAllocation =
        allocationService.getRootAllocation(testHelper.retrieveBusiness().getId()).allocation();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(), "", rootAllocation.getId(), testHelper.createUser(business).user());
    Cookie authCookie = testHelper.login(rootAllocation.getOwnerId());

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format("/allocations/%s", allocationRecord.allocation().getId()))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getAllocationChildren_success() {
    Business business = testHelper.retrieveBusiness();
    final Allocation rootAllocation =
        allocationService.getRootAllocation(testHelper.retrieveBusiness().getId()).allocation();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(), "", rootAllocation.getId(), testHelper.createUser(business).user());
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            parentAllocationRecord.allocation().getId(),
            testHelper.createUser(business).user());

    Cookie authCookie = testHelper.login(rootAllocation.getOwnerId());

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format(
                        "/allocations/%s/children", parentAllocationRecord.allocation().getId()))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void reallocateAllocationFunds_success() {
    Business business = testHelper.retrieveBusiness();
    BusinessBankAccount businessBankAccount = testHelper.retrieveBusinessBankAccount();
    com.clearspend.capital.common.data.model.Amount amount =
        new com.clearspend.capital.common.data.model.Amount(
            business.getCurrency(), BigDecimal.valueOf(100));
    AdjustmentAndHoldRecord deposit =
        testHelper.transactBankAccount(
            businessBankAccount, BankAccountTransactType.DEPOSIT, amount.getAmount(), false);
    final Allocation rootAllocation =
        allocationService.getRootAllocation(testHelper.retrieveBusiness().getId()).allocation();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(), "", rootAllocation.getId(), testHelper.createUser(business).user());
    Card card =
        testHelper.issueCard(
            business,
            allocationRecord.allocation(),
            testHelper.createUser(business).user(),
            amount.getCurrency(),
            FundingType.INDIVIDUAL,
            CardType.PHYSICAL,
            false);
    Account businessAccount = allocationService.getRootAllocation(business.getId()).account();
    AccountReallocateFundsRecord reallocateFundsRecord =
        accountService.reallocateFunds(
            businessAccount.getId(), allocationRecord.account().getId(), amount);

    AllocationFundCardRequest request =
        new AllocationFundCardRequest(
            allocationRecord.account().getId(),
            card.getId(),
            AllocationReallocationType.ALLOCATION_TO_CARD,
            Amount.of(amount));

    String body = objectMapper.writeValueAsString(request);
    Cookie authCookie = testHelper.login(rootAllocation.getOwnerId());

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format(
                        "/allocations/%s/transactions", allocationRecord.allocation().getId()))
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  @SneakyThrows
  @Test
  void createAllocationWithAStartAmount_success() {
    Business business = testHelper.retrieveBusiness();
    BusinessBankAccount businessBankAccount = testHelper.retrieveBusinessBankAccount();
    com.clearspend.capital.common.data.model.Amount amount =
        new com.clearspend.capital.common.data.model.Amount(
            business.getCurrency(), BigDecimal.valueOf(100));
    testHelper.transactBankAccount(
        businessBankAccount, BankAccountTransactType.DEPOSIT, amount.getAmount(), false);
    final Allocation rootAllocation =
        allocationService.getRootAllocation(business.getId()).allocation();
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            testHelper.generateFullName(),
            rootAllocation.getId(),
            testHelper.createUser(testHelper.retrieveBusiness()).user().getId(),
            new Amount(Currency.USD, BigDecimal.valueOf(10)),
            Collections.singletonList(new CurrencyLimit(Currency.USD, new HashMap<>())),
            Collections.emptyList(),
            Collections.emptySet());

    String body = objectMapper.writeValueAsString(request);
    Cookie authCookie = testHelper.login(rootAllocation.getOwnerId());

    MockHttpServletResponse response =
        mvc.perform(
                post("/allocations")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @Test
  @SneakyThrows
  void updateAllocation_success() {
    // given
    Business business = testHelper.retrieveBusiness();
    final Allocation rootAllocation =
        allocationService.getRootAllocation(testHelper.retrieveBusiness().getId()).allocation();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            faker.name().name(),
            rootAllocation.getId(),
            testHelper.createUser(business).user());

    UpdateAllocationRequest updateAllocationRequest = new UpdateAllocationRequest();
    updateAllocationRequest.setName("Changed name");
    updateAllocationRequest.setOwnerId(testHelper.createUser(business).user().getId());
    updateAllocationRequest.setLimits(
        Collections.singletonList(
            new CurrencyLimit(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(LimitPeriod.DAILY, new Limit(BigDecimal.TEN, BigDecimal.ZERO))))));
    updateAllocationRequest.setDisabledMccGroups(
        mccGroupService.retrieveMccGroups(business.getId()).stream()
            .map(MccGroup::getId)
            .collect(Collectors.toList()));
    updateAllocationRequest.setDisabledTransactionChannels(
        Collections.singleton(TransactionChannel.MOTO));

    Cookie authCookie = testHelper.login(rootAllocation.getOwnerId());

    // when
    MockHttpServletResponse response =
        mvc.perform(
                patch("/allocations/" + allocationRecord.allocation().getId())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(updateAllocationRequest))
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    AllocationDetailsResponse allocationDetailsResponse =
        objectMapper.readValue(response.getContentAsString(), AllocationDetailsResponse.class);

    // then
    assertThat(allocationDetailsResponse.getAllocation().getAllocationId())
        .isEqualTo(allocationRecord.allocation().getId());
    assertThat(allocationDetailsResponse.getAllocation().getName())
        .isEqualTo(updateAllocationRequest.getName());
    assertThat(allocationDetailsResponse.getAllocation().getOwnerId())
        .isEqualTo(updateAllocationRequest.getOwnerId());
    assertThat(allocationDetailsResponse.getOwner()).isNotNull();
    assertThat(allocationDetailsResponse.getLimits())
        .isEqualTo(updateAllocationRequest.getLimits());
    assertThat(allocationDetailsResponse.getDisabledMccGroups())
        .isEqualTo(updateAllocationRequest.getDisabledMccGroups());
    assertThat(allocationDetailsResponse.getDisabledTransactionChannels())
        .isEqualTo(updateAllocationRequest.getDisabledTransactionChannels());
  }
}
