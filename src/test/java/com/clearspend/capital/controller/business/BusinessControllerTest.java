package com.clearspend.capital.controller.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.controller.type.allocation.SearchBusinessAllocationRequest;
import com.clearspend.capital.controller.type.business.BusinessLimit;
import com.clearspend.capital.controller.type.business.reallocation.BusinessFundAllocationResponse;
import com.clearspend.capital.controller.type.business.reallocation.BusinessReallocationRequest;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class BusinessControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mvcHelper;
  private final TestHelper testHelper;

  private final BusinessService businessService;
  private final UserRepository userRepository;

  private final AccountService accountService;
  private final AllocationService allocationService;

  private Cookie authCookie;
  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.init();
    authCookie = createBusinessRecord.authCookie();
    testHelper.setCurrentUser(createBusinessRecord.user());
  }

  @SneakyThrows
  @Test
  void getBusiness_success() {
    Business business = createBusinessRecord.business();

    MockHttpServletResponse response =
        mvc.perform(get("/businesses/").contentType("application/json").cookie(this.authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    com.clearspend.capital.controller.type.business.Business jsonBusiness =
        objectMapper.readValue(
            response.getContentAsString(),
            com.clearspend.capital.controller.type.business.Business.class);

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
  public void reallocateBusinessFunds_success() {
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            testHelper.generateAllocationName(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    accountService.depositFunds(
        createBusinessRecord.business().getId(),
        createBusinessRecord.allocationRecord().account(),
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false,
        true);

    // move $100 from root allocation (balance $1000) to newly created allocation (balance $0)
    BusinessReallocationRequest request =
        new BusinessReallocationRequest(
            createBusinessRecord.allocationRecord().allocation().getId(),
            allocationRecord.allocation().getId(),
            com.clearspend.capital.controller.type.Amount.of(
                new Amount(Currency.USD, BigDecimal.valueOf(100))));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse mockHttpServletResponse =
        mvc.perform(
                post("/businesses/transactions")
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    BusinessFundAllocationResponse response =
        objectMapper.readValue(
            mockHttpServletResponse.getContentAsString(), BusinessFundAllocationResponse.class);
    assertEquals(
        900.00,
        response.getLedgerBalanceFrom().getAmount().doubleValue(),
        "The businessLedgerBalance result is not as expected");
    assertEquals(
        100.00,
        response.getLedgerBalanceTo().getAmount().doubleValue(),
        "The allocationLedgerBalance result is not as expected");

    // move $90 from new allocation (balance $100) to root allocation (balance $900)
    request =
        new BusinessReallocationRequest(
            allocationRecord.allocation().getId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            com.clearspend.capital.controller.type.Amount.of(
                new Amount(Currency.USD, BigDecimal.valueOf(90))));

    body = objectMapper.writeValueAsString(request);

    mockHttpServletResponse =
        mvc.perform(
                post("/businesses/transactions")
                    .content(body)
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    response =
        objectMapper.readValue(
            mockHttpServletResponse.getContentAsString(), BusinessFundAllocationResponse.class);
    assertEquals(
        10.00,
        response.getLedgerBalanceFrom().getAmount().doubleValue(),
        "The businessLedgerBalance result is not as expected");
    assertEquals(
        990.00,
        response.getLedgerBalanceTo().getAmount().doubleValue(),
        "The allocationLedgerBalance result is not as expected");
  }

  @SneakyThrows
  @Test
  public void getBusinessAllocations_success() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness(100L);
    Allocation rootAllocation = businessRecord.allocationRecord().allocation();
    AllocationRecord allocationChild1 =
        testHelper.createAllocation(
            businessRecord.business().getId(),
            "child_1",
            rootAllocation.getId(),
            businessRecord.user());
    AllocationRecord allocationGrandchild1 =
        testHelper.createAllocation(
            businessRecord.business().getId(),
            "grandchild_1",
            allocationChild1.allocation().getId(),
            businessRecord.user());
    AllocationRecord allocationGrandchild2 =
        testHelper.createAllocation(
            businessRecord.business().getId(),
            "grandchild_2",
            allocationChild1.allocation().getId(),
            businessRecord.user());

    accountService.depositFunds(
        businessRecord.business().getId(),
        allocationService.getRootAllocation(businessRecord.business().getId()).account(),
        Amount.of(Currency.USD, new BigDecimal(200)),
        true,
        true);

    MockHttpServletResponse response =
        mvc.perform(
                get("/businesses/allocations")
                    .header("businessId", businessRecord.business().getId())
                    .contentType(APPLICATION_JSON_VALUE)
                    .cookie(businessRecord.authCookie()))
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
    assertThat(allocationMap.get(rootAllocation.getId()).getParentAllocationId()).isNull();
    assertThat(allocationMap.get(rootAllocation.getId()).getChildrenAllocationIds())
        .containsExactly(allocationChild1.allocation().getId());
    assertThat(
            allocationMap.get(rootAllocation.getId()).getAccount().getLedgerBalance().getAmount())
        .isEqualTo(new BigDecimal("300.00"));
    assertThat(
            allocationMap
                .get(rootAllocation.getId())
                .getAccount()
                .getAvailableBalance()
                .getAmount())
        .isEqualTo(new BigDecimal("100.00"));

    // child 1
    assertThat(allocationMap.get(allocationChild1.allocation().getId()).getParentAllocationId())
        .isEqualTo(rootAllocation.getId());
    assertThat(allocationMap.get(allocationChild1.allocation().getId()).getChildrenAllocationIds())
        .containsExactlyInAnyOrder(
            allocationGrandchild1.allocation().getId(), allocationGrandchild2.allocation().getId());

    // grandchild 1
    assertThat(
            allocationMap.get(allocationGrandchild1.allocation().getId()).getParentAllocationId())
        .isEqualTo(allocationChild1.allocation().getId());
    assertThat(
            allocationMap
                .get(allocationGrandchild1.allocation().getId())
                .getChildrenAllocationIds())
        .isEmpty();

    // grandchild 2
    assertThat(
            allocationMap.get(allocationGrandchild2.allocation().getId()).getParentAllocationId())
        .isEqualTo(allocationChild1.allocation().getId());
    assertThat(
            allocationMap
                .get(allocationGrandchild2.allocation().getId())
                .getChildrenAllocationIds())
        .isEmpty();

    log.info(String.valueOf(allocations));
  }

  @SneakyThrows
  @Test
  public void getRootAllocation_ForUnknownBusinessId_expectStatus204() {
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
        .andExpect(status().isNotFound())
        .andReturn()
        .getResponse();
  }

  @SneakyThrows
  @Test
  public void searchBusinessAllocation_success() {
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
    Business business = createBusinessRecord.business();
    accountService.depositFunds(
        business.getId(),
        allocationService.getRootAllocation(business.getId()).account(),
        Amount.of(Currency.USD, new BigDecimal(200)),
        true,
        true);
    MockHttpServletResponse response =
        mvc.perform(get("/businesses/accounts").contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    Account account = objectMapper.readValue(response.getContentAsString(), Account.class);
    assertThat(
            com.clearspend.capital.controller.type.Amount.of(
                Amount.of(Currency.USD, BigDecimal.valueOf(200))))
        .isEqualTo(account.getLedgerBalance());
  }

  @Test
  void defaultBusinessLimit() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);
    BusinessLimit businessLimit =
        mvcHelper.queryObject(
            "/businesses/business-limit", HttpMethod.GET, authCookie, BusinessLimit.class);

    assertThat(businessLimit.getIssuedPhysicalCardsLimit()).isEqualTo(10);
    assertThat(businessLimit.getIssuedPhysicalCardsTotal()).isEqualTo(0);
  }

  @Test
  @SneakyThrows
  void completeOnboarding() {
    businessService.updateBusiness(
        createBusinessRecord.business().getId(),
        BusinessStatus.ONBOARDING,
        BusinessOnboardingStep.LINK_ACCOUNT,
        KnowYourBusinessStatus.PASS);

    mvc.perform(
            post("/businesses/complete-onboarding")
                .contentType("application/json")
                .cookie(authCookie))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.COMPLETE);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.ACTIVE);
  }
}
