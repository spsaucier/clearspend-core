package com.tranwall.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.controller.type.Address;
import com.tranwall.capital.controller.type.allocation.Allocation;
import com.tranwall.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.AccountService;
import com.tranwall.capital.service.AllocationService;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class BusinessControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final UserRepository userRepository;

  private final AccountService accountService;
  private final AllocationService allocationService;

  private Cookie authCookie;

  @BeforeEach
  void init() {
    testHelper.init();
    this.authCookie = testHelper.login("business-owner-tester@clearspend.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void getBusiness_success() {
    Business business = testHelper.retrieveBusiness();

    MockHttpServletResponse response =
        mvc.perform(get("/businesses/").contentType("application/json").cookie(this.authCookie))
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
  public void getRootAllocation_success() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    MockHttpServletResponse response =
        mvc.perform(
                get("/businesses/allocations")
                    .header("businessId", createBusinessRecord.business().getId())
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    Allocation responseAllocation =
        objectMapper.readValue(response.getContentAsString(), Allocation.class);
    log.info(String.valueOf(responseAllocation));
  }

  @SneakyThrows
  @Test
  public void getRootAllocation_ForUnknownBusinessId_expectStatus204() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    testHelper.deleteBusinessOwner(createBusinessRecord.businessOwner().getId());
    testHelper.deleteAllocation(business.getId());
    testHelper.deleteAccount(business.getId());
    testHelper.deleteUser(userRepository.findByBusinessId(business.getId()).get(0).getId());
    testHelper.deleteBusinessLimit(business.getId());
    testHelper.deleteSpendLimit(business.getId());
    testHelper.deleteBusiness(business.getId());

    mvc.perform(
            get("/businesses/allocations")
                .contentType(APPLICATION_JSON_VALUE)
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isNoContent())
        .andReturn()
        .getResponse();
  }

  @SneakyThrows
  @Test
  public void searchBusinessAllocation_success() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    SearchBusinessAllocationRequest request =
        new SearchBusinessAllocationRequest(
            createBusinessRecord.allocationRecord().allocation().getName());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/businesses/allocations")
                    .header("businessId", createBusinessRecord.business().getId())
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<Allocation> responseAllocationList =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    assertEquals(1, responseAllocationList.size(), "The expected result is not ok");
  }

  @SneakyThrows
  @Test
  void getBusinessAccountWithFetchHoldTrueWhenAmountIsAddedOnHold() {
    Business business = testHelper.retrieveBusiness();
    accountService.depositFunds(
        business.getId(),
        allocationService.getRootAllocation(business.getId()).account(),
        allocationService.getRootAllocation(business.getId()).allocation(),
        com.tranwall.capital.common.data.model.Amount.of(Currency.USD, new BigDecimal(200)),
        true);
    MockHttpServletResponse response =
        mvc.perform(get("/businesses/accounts").contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    com.tranwall.capital.controller.type.account.Account account =
        objectMapper.readValue(
            response.getContentAsString(),
            com.tranwall.capital.controller.type.account.Account.class);
    Assertions.assertEquals(
        com.tranwall.capital.controller.type.Amount.of(
            com.tranwall.capital.common.data.model.Amount.of(
                Currency.USD, BigDecimal.valueOf(200))),
        account.getLedgerBalance());
  }
}
