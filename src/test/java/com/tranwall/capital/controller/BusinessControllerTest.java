package com.tranwall.capital.controller;

import static java.math.BigDecimal.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.controller.type.Address;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.allocation.Allocation;
import com.tranwall.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.tranwall.capital.controller.type.business.reallocation.BusinessFundAllocationRequest;
import com.tranwall.capital.controller.type.business.reallocation.BusinessFundAllocationResponse;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import java.math.BigDecimal;
import java.util.List;
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
public class BusinessControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final AccountService accountService;

  @BeforeEach
  void init() {
    testHelper.init();
  }

  @SneakyThrows
  @Test
  void getBusiness_success() {
    Business business = testHelper.retrieveBusiness();

    MockHttpServletResponse response =
        mvc.perform(
                get("/businesses/" + business.getId())
                    .header("businessId", business.getId().toString())
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    com.tranwall.capital.controller.type.business.Business jsonBusiness =
        objectMapper.readValue(
            response.getContentAsString(),
            com.tranwall.capital.controller.type.business.Business.class);

    assertThat(jsonBusiness.getBusinessId()).isEqualTo(business.getId());
    assertThat(jsonBusiness.getLegalName()).isEqualTo(business.getLegalName());
    assertThat(jsonBusiness.getBusinessType()).isEqualTo(business.getType());
    assertThat(jsonBusiness.getEmployerIdentificationNumber())
        .isEqualTo(business.getEmployerIdentificationNumber());
    assertThat(jsonBusiness.getBusinessPhone())
        .isEqualTo(business.getBusinessPhone().getEncrypted());
    assertThat(jsonBusiness.getAddress()).isEqualTo(new Address(business.getClearAddress()));
    assertThat(jsonBusiness.getOnboardingStep()).isEqualTo(business.getOnboardingStep());
    assertThat(jsonBusiness.getKnowYourBusinessStatus())
        .isEqualTo(business.getKnowYourBusinessStatus());
    assertThat(jsonBusiness.getStatus()).isEqualTo(business.getStatus());
  }

  @SneakyThrows
  @Test
  public void reallocateBusinessFundsByWithdrawFromBusiness_success() {
    Program program = testHelper.retrievePooledProgram();
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();

    accountService.depositFunds(
        business.getId(),
        com.tranwall.capital.common.data.model.Amount.of(Currency.USD, new BigDecimal("1000")),
        false);

    AllocationRecord allocationRecord = businessAndAllocationsRecord.allocationRecords().get(0);

    BusinessFundAllocationRequest request =
        new BusinessFundAllocationRequest(
            allocationRecord.allocation().getId(),
            allocationRecord.account().getId(),
            FundsTransactType.WITHDRAW,
            new Amount(Currency.USD, valueOf(100)));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse mockHttpServletResponse =
        mvc.perform(
                post("/businesses/transactions")
                    .header("businessId", business.getId())
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    BusinessFundAllocationResponse responseDto =
        objectMapper.readValue(
            mockHttpServletResponse.getContentAsString(), BusinessFundAllocationResponse.class);
    assertEquals(
        900.00,
        responseDto.getBusinessLedgerBalance().getAmount().doubleValue(),
        "The businessLedgerBalance result is not as expected");
    assertEquals(
        100.00,
        responseDto.getAllocationLedgerBalance().getAmount().doubleValue(),
        "The allocationLedgerBalance result is not as expected");
  }

  @SneakyThrows
  @Test
  public void reallocateBusinessFundsByDepositToBusiness_success() {
    Program program = testHelper.retrievePooledProgram();
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();

    accountService.depositFunds(
        business.getId(),
        com.tranwall.capital.common.data.model.Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveBusinessAccount(business.getId(), business.getCurrency(), false);
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), "", null);
    accountService.reallocateFunds(
        account.getId(),
        parentAllocationRecord.account().getId(),
        new com.tranwall.capital.common.data.model.Amount(Currency.USD, valueOf(300)));

    BusinessFundAllocationRequest businessFundAllocationRequest =
        new BusinessFundAllocationRequest(
            parentAllocationRecord.allocation().getId(),
            parentAllocationRecord.account().getId(),
            FundsTransactType.DEPOSIT,
            new Amount(Currency.USD, valueOf(100)));

    String body = objectMapper.writeValueAsString(businessFundAllocationRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/businesses/transactions")
                    .header("businessId", business.getId())
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    BusinessFundAllocationResponse responseDto =
        objectMapper.readValue(response.getContentAsString(), BusinessFundAllocationResponse.class);
    assertEquals(
        200.00,
        responseDto.getBusinessLedgerBalance().getAmount().doubleValue(),
        "Wrong expected result");
    assertEquals(
        800.00,
        responseDto.getAllocationLedgerBalance().getAmount().doubleValue(),
        "Wrong expected result");
  }

  @SneakyThrows
  @Test
  public void reallocateBusinessFunds_FailWhenNotSufficientBalance() {
    Program program = testHelper.retrievePooledProgram();
    Business business = testHelper.retrieveBusiness();
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), "", null);
    BusinessFundAllocationRequest businessFundAllocationRequest =
        new BusinessFundAllocationRequest(
            parentAllocationRecord.allocation().getId(),
            parentAllocationRecord.account().getId(),
            FundsTransactType.DEPOSIT,
            new Amount(Currency.USD, valueOf(100)));

    String body = objectMapper.writeValueAsString(businessFundAllocationRequest);

    mvc.perform(
            post("/businesses/transactions")
                .header("businessId", business.getId())
                .content(body)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().is4xxClientError())
        .andReturn()
        .getResponse();
  }

  @SneakyThrows
  @Test
  public void getRootAllocation_success() {
    Program program = testHelper.retrieveIndividualProgram();
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    testHelper.createAllocation(
        program.getId(), businessAndAllocationsRecord.business().getId(), "", null);

    MockHttpServletResponse response =
        mvc.perform(
                get("/businesses/allocations")
                    .header("businessId", businessAndAllocationsRecord.business().getId())
                    .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<Allocation> responseAllocationList =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    assertEquals(2, responseAllocationList.size(), "The expected result is not ok");
  }

  @SneakyThrows
  @Test
  public void getRootAllocation_ForUnknownBusinessId_expectStatus500InvalidUUIDString() {
    mvc.perform(
            get("/businesses/allocations")
                .header("businessId", "businessId")
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().is5xxServerError())
        .andReturn()
        .getResponse();
  }

  @SneakyThrows
  @Test
  public void searchBusinessAllocation_success() {
    Program program = testHelper.retrieveIndividualProgram();
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            program.getId(),
            businessAndAllocationsRecord.business().getId(),
            "12345HelloWorld09876",
            null);

    SearchBusinessAllocationRequest request = new SearchBusinessAllocationRequest("45Hell");

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/businesses/allocations")
                    .header("businessId", businessAndAllocationsRecord.business().getId())
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<Allocation> responseAllocationList =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    assertEquals(1, responseAllocationList.size(), "The expected result is not ok");
    assertThat(responseAllocationList.get(0).getAllocationId())
        .isEqualTo(allocationRecord.allocation().getId());
  }
}
