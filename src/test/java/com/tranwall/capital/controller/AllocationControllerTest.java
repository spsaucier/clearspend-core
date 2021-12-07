package com.tranwall.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javafaker.Faker;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.controller.type.allocation.AllocationDetails;
import com.tranwall.capital.controller.type.allocation.AllocationFundCardRequest;
import com.tranwall.capital.controller.type.allocation.CreateAllocationRequest;
import com.tranwall.capital.controller.type.allocation.UpdateAllocationRequest;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AllocationReallocationType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.math.BigDecimal;
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

  private final Faker faker = new Faker();

  private Cookie authCookie;

  @BeforeEach
  void init() {
    testHelper.init();
    this.authCookie = testHelper.login("business-owner-tester@clearspend.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void createAllocation_success() {
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            testHelper.generateFullName(),
            allocationService
                .getRootAllocation(testHelper.retrieveBusiness().getId())
                .allocation()
                .getId(),
            testHelper.createUser(testHelper.retrieveBusiness()).user().getId(),
            new com.tranwall.capital.controller.type.Amount(Currency.USD, BigDecimal.ZERO));

    String body = objectMapper.writeValueAsString(request);

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
    Program program = testHelper.retrievePooledProgram();
    Business business = testHelper.retrieveBusiness();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            allocationService
                .getRootAllocation(testHelper.retrieveBusiness().getId())
                .allocation()
                .getId(),
            testHelper.createUser(business).user());

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
    Program program = testHelper.retrievePooledProgram();
    Business business = testHelper.retrieveBusiness();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            allocationService
                .getRootAllocation(testHelper.retrieveBusiness().getId())
                .allocation()
                .getId(),
            testHelper.createUser(business).user());
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            parentAllocationRecord.allocation().getId(),
            testHelper.createUser(business).user());

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
    Program program = testHelper.retrieveIndividualProgram();
    Business business = testHelper.retrieveBusiness();
    BusinessBankAccount businessBankAccount = testHelper.retrieveBusinessBankAccount();
    Amount amount = new Amount(business.getCurrency(), BigDecimal.valueOf(100));
    AdjustmentAndHoldRecord deposit =
        testHelper.transactBankAccount(BankAccountTransactType.DEPOSIT, amount.getAmount(), false);
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            "",
            allocationService
                .getRootAllocation(testHelper.retrieveBusiness().getId())
                .allocation()
                .getId(),
            testHelper.createUser(business).user());
    Card card =
        testHelper.issueCard(
            business,
            allocationRecord.allocation(),
            testHelper.createUser(business).user(),
            program,
            amount.getCurrency());
    Account businessAccount = allocationService.getRootAllocation(business.getId()).account();
    AccountReallocateFundsRecord reallocateFundsRecord =
        accountService.reallocateFunds(
            businessAccount.getId(), allocationRecord.account().getId(), amount);

    AllocationFundCardRequest request =
        new AllocationFundCardRequest(
            allocationRecord.account().getId(),
            card.getId(),
            AllocationReallocationType.ALLOCATION_TO_CARD,
            com.tranwall.capital.controller.type.Amount.of(amount));

    String body = objectMapper.writeValueAsString(request);

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
    Amount amount = new Amount(business.getCurrency(), BigDecimal.valueOf(100));
    testHelper.transactBankAccount(BankAccountTransactType.DEPOSIT, amount.getAmount(), false);
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            testHelper.generateFullName(),
            allocationService.getRootAllocation(business.getId()).allocation().getId(),
            testHelper.createUser(testHelper.retrieveBusiness()).user().getId(),
            new com.tranwall.capital.controller.type.Amount(Currency.USD, BigDecimal.valueOf(10)));

    String body = objectMapper.writeValueAsString(request);

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
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            faker.name().name(),
            allocationService
                .getRootAllocation(testHelper.retrieveBusiness().getId())
                .allocation()
                .getId(),
            testHelper.createUser(business).user());

    UpdateAllocationRequest updateAllocationRequest = new UpdateAllocationRequest();
    updateAllocationRequest.setName("Changed name");
    updateAllocationRequest.setOwnerId(testHelper.createUser(business).user().getId());

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

    AllocationDetails allocationDetails =
        objectMapper.readValue(response.getContentAsString(), AllocationDetails.class);

    // then
    assertThat(allocationDetails.getAllocation().getAllocationId())
        .isEqualTo(allocationRecord.allocation().getId());
    assertThat(allocationDetails.getAllocation().getName())
        .isEqualTo(updateAllocationRequest.getName());
    assertThat(allocationDetails.getAllocation().getOwnerId())
        .isEqualTo(allocationRecord.allocation().getOwnerId());
    assertThat(allocationDetails.getOwner()).isNotNull();
  }
}
