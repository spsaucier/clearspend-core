package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.controller.type.allocation.AllocationFundCardRequest;
import com.tranwall.capital.controller.type.allocation.CreateAllocationRequest;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.math.BigDecimal;
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
class AllocationControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final AccountService accountService;

  @BeforeEach
  void init() {
    testHelper.init();
  }

  @SneakyThrows
  @Test
  void createAllocation_success() {
    Program program = testHelper.retrievePooledProgram();
    Business business = testHelper.retrieveBusiness();
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            program.getId(),
            testHelper.generateFullName(),
            new com.tranwall.capital.controller.type.Amount(Currency.USD, BigDecimal.ZERO));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/allocations")
                    .header("businessId", business.getId().toString())
                    .contentType("application/json")
                    .content(body))
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
        testHelper.createAllocation(program.getId(), business.getId(), "", null);

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format("/allocations/%s", allocationRecord.allocation().getId()))
                    .header("businessId", business.getId().toString())
                    .contentType("application/json"))
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
        testHelper.createAllocation(program.getId(), business.getId(), "", null);
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            program.getId(), business.getId(), "", parentAllocationRecord.allocation().getId());

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format(
                        "/allocations/%s/children", parentAllocationRecord.allocation().getId()))
                    .header("businessId", business.getId().toString())
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void reallocateAllocationFunds_success() {
    Bin bin = testHelper.retrieveBin();
    Program program = testHelper.retrieveIndividualProgram();
    Business business = testHelper.retrieveBusiness();
    BusinessBankAccount businessBankAccount = testHelper.retrieveBusinessBankAccount();
    Amount amount = new Amount(business.getCurrency(), BigDecimal.valueOf(100));
    AdjustmentRecord deposit =
        testHelper.transactBankAccount(FundsTransactType.DEPOSIT, amount.getAmount(), false);
    AllocationRecord allocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), "", null);
    Card card =
        testHelper.issueCard(
            business,
            allocationRecord.allocation(),
            testHelper.createUser(business).user(),
            bin,
            program,
            amount.getCurrency());
    Account businessAccount =
        accountService.retrieveBusinessAccount(business.getId(), business.getCurrency(), false);
    AccountReallocateFundsRecord reallocateFundsRecord =
        accountService.reallocateFunds(
            businessAccount.getId(), allocationRecord.account().getId(), amount);

    AllocationFundCardRequest request =
        new AllocationFundCardRequest(
            allocationRecord.account().getId(),
            card.getId(),
            FundsTransactType.DEPOSIT,
            com.tranwall.capital.controller.type.Amount.of(amount));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format(
                        "/allocations/%s/transactions", allocationRecord.allocation().getId()))
                    .header("businessId", business.getId().toString())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  @SneakyThrows
  @Test
  void createAllocationWithAStartAmount_success() {
    Program program = testHelper.retrievePooledProgram();
    Business business = testHelper.retrieveBusiness();
    Amount amount = new Amount(business.getCurrency(), BigDecimal.valueOf(100));
    testHelper.transactBankAccount(FundsTransactType.DEPOSIT, amount.getAmount(), false);
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            program.getId(),
            testHelper.generateFullName(),
            new com.tranwall.capital.controller.type.Amount(Currency.USD, BigDecimal.valueOf(10)));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/allocations")
                    .header("businessId", business.getId().toString())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }
}
