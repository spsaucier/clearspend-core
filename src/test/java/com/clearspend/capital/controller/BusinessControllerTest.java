package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    com.clearspend.capital.controller.type.business.Business jsonBusiness =
        objectMapper.readValue(
            response.getContentAsString(),
            com.clearspend.capital.controller.type.business.Business.class);

    org.assertj.core.api.Assertions.assertThat(jsonBusiness.getBusinessId())
        .isEqualTo(business.getId());
    assertThat(jsonBusiness.getLegalName()).isEqualTo(business.getLegalName());
    org.assertj.core.api.Assertions.assertThat(jsonBusiness.getBusinessType())
        .isEqualTo(business.getType());
    assertThat(jsonBusiness.getEmployerIdentificationNumber())
        .isEqualTo(business.getEmployerIdentificationNumber());
    assertThat(jsonBusiness.getBusinessPhone())
        .isEqualTo(business.getBusinessPhone().getEncrypted());
    org.assertj.core.api.Assertions.assertThat(jsonBusiness.getAddress())
        .isEqualTo(new Address(business.getClearAddress()));
    org.assertj.core.api.Assertions.assertThat(jsonBusiness.getOnboardingStep())
        .isEqualTo(business.getOnboardingStep());
    org.assertj.core.api.Assertions.assertThat(jsonBusiness.getKnowYourBusinessStatus())
        .isEqualTo(business.getKnowYourBusinessStatus());
    org.assertj.core.api.Assertions.assertThat(jsonBusiness.getStatus())
        .isEqualTo(business.getStatus());
  }

  @SneakyThrows
  @Test
  public void getBusinessAllocations_success() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();
    AllocationRecord allocationChild1 =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "child_1",
            rootAllocation.getId(),
            createBusinessRecord.user());
    AllocationRecord allocationGrandchild1 =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "grandchild_1",
            allocationChild1.allocation().getId(),
            createBusinessRecord.user());
    AllocationRecord allocationGrandchild2 =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "grandchild_2",
            allocationChild1.allocation().getId(),
            createBusinessRecord.user());

    MockHttpServletResponse response =
        mvc.perform(
                get("/businesses/allocations")
                    .header("businessId", createBusinessRecord.business().getId())
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<com.clearspend.capital.controller.type.allocation.Allocation> allocations =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    assertThat(allocations)
        .extracting("name")
        .containsExactlyInAnyOrder(
            rootAllocation.getName(),
            allocationChild1.allocation().getName(),
            allocationGrandchild1.allocation().getName(),
            allocationGrandchild2.allocation().getName());

    Map<TypedId<AllocationId>, com.clearspend.capital.controller.type.allocation.Allocation>
        allocationMap =
            allocations.stream()
                .collect(
                    Collectors.toMap(
                        com.clearspend.capital.controller.type.allocation.Allocation
                            ::getAllocationId,
                        Function.identity()));

    // checking tree structure
    // root node
    org.assertj.core.api.Assertions.assertThat(
            allocationMap.get(rootAllocation.getId()).getParentAllocationId())
        .isNull();
    org.assertj.core.api.Assertions.assertThat(
            allocationMap.get(rootAllocation.getId()).getChildrenAllocationIds())
        .containsExactly(allocationChild1.allocation().getId());

    // child 1
    org.assertj.core.api.Assertions.assertThat(
            allocationMap.get(allocationChild1.allocation().getId()).getParentAllocationId())
        .isEqualTo(rootAllocation.getId());
    org.assertj.core.api.Assertions.assertThat(
            allocationMap.get(allocationChild1.allocation().getId()).getChildrenAllocationIds())
        .containsExactlyInAnyOrder(
            allocationGrandchild1.allocation().getId(), allocationGrandchild2.allocation().getId());

    // grandchild 1
    org.assertj.core.api.Assertions.assertThat(
            allocationMap.get(allocationGrandchild1.allocation().getId()).getParentAllocationId())
        .isEqualTo(allocationChild1.allocation().getId());
    org.assertj.core.api.Assertions.assertThat(
            allocationMap
                .get(allocationGrandchild1.allocation().getId())
                .getChildrenAllocationIds())
        .isEmpty();

    // grandchild 2
    org.assertj.core.api.Assertions.assertThat(
            allocationMap.get(allocationGrandchild2.allocation().getId()).getParentAllocationId())
        .isEqualTo(allocationChild1.allocation().getId());
    org.assertj.core.api.Assertions.assertThat(
            allocationMap
                .get(allocationGrandchild2.allocation().getId())
                .getChildrenAllocationIds())
        .isEmpty();

    log.info(String.valueOf(allocations));
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

    List<com.clearspend.capital.controller.type.allocation.Allocation> responseAllocationList =
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
        Amount.of(Currency.USD, new BigDecimal(200)),
        true);
    MockHttpServletResponse response =
        mvc.perform(get("/businesses/accounts").contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    Account account = objectMapper.readValue(response.getContentAsString(), Account.class);
    Assertions.assertEquals(
        com.clearspend.capital.controller.type.Amount.of(
            Amount.of(Currency.USD, BigDecimal.valueOf(200))),
        account.getLedgerBalance());
  }
}
