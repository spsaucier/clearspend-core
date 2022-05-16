package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.common.advice.GlobalControllerExceptionHandler.ControllerError;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.allocation.AllocationDetailsResponse;
import com.clearspend.capital.controller.type.allocation.AllocationFundCardRequest;
import com.clearspend.capital.controller.type.allocation.CreateAllocationRequest;
import com.clearspend.capital.controller.type.allocation.StopAllCardsRequest;
import com.clearspend.capital.controller.type.allocation.StopAllCardsRequest.StopPhysicalCardsType;
import com.clearspend.capital.controller.type.allocation.StopAllCardsResponse;
import com.clearspend.capital.controller.type.allocation.UpdateAllocationRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.card.limits.Limit;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.util.function.ThrowableFunctions.ThrowingFunction;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class AllocationControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mockMvcHelper;
  private final TestHelper testHelper;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final AllocationRepository allocationRepository;
  private final PermissionValidationHelper permissionValidationHelper;
  private final StripeMockClient stripeMockClient;
  private final CardRepository cardRepository;
  private final ServiceHelper serviceHelper;

  private final Faker faker = new Faker();
  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.init();
  }

  @AfterEach
  void cleanup() {
    stripeMockClient.reset();
  }

  private Card issueCardForStopAll(final Allocation allocation, final CardType cardType) {
    return testHelper.issueCard(
        createBusinessRecord.business(),
        allocation,
        createBusinessRecord.user(),
        Currency.USD,
        FundingType.POOLED,
        cardType,
        false);
  }

  @Test
  @SneakyThrows
  void stopAllCards_AllocationIsArchived() {
    createBusinessRecord.allocationRecord().allocation().setArchived(true);
    allocationRepository.saveAndFlush(createBusinessRecord.allocationRecord().allocation());

    final StopAllCardsRequest request =
        new StopAllCardsRequest(false, true, StopPhysicalCardsType.CANCEL);

    final String response =
        mockMvcHelper
            .query(
                "/allocations/%s/cards/stop"
                    .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
                HttpMethod.PATCH,
                createBusinessRecord.authCookie(),
                request)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "Allocation is archived");
  }

  @Test
  @SneakyThrows
  void updateAllocation_AllocationIsArchived() {
    createBusinessRecord.allocationRecord().allocation().setArchived(true);
    allocationRepository.saveAndFlush(createBusinessRecord.allocationRecord().allocation());
    final UpdateAllocationRequest request = new UpdateAllocationRequest();
    final String response =
        mockMvcHelper
            .query(
                "/allocations/%s"
                    .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
                HttpMethod.PATCH,
                createBusinessRecord.authCookie(),
                request)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "Allocation is archived");
  }

  @Test
  @SneakyThrows
  void reallocateAllocationFunds_AllocationIsArchived() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Card card =
        issueCardForStopAll(createBusinessRecord.allocationRecord().allocation(), CardType.VIRTUAL);
    final AllocationFundCardRequest request =
        new AllocationFundCardRequest(
            createBusinessRecord.allocationRecord().account().getId(),
            card.getId(),
            AllocationReallocationType.ALLOCATION_TO_CARD,
            new Amount(Currency.USD, new BigDecimal("10")));
    createBusinessRecord.allocationRecord().allocation().setArchived(true);
    allocationRepository.saveAndFlush(createBusinessRecord.allocationRecord().allocation());

    final String response =
        mockMvcHelper
            .query(
                "/allocations/%s/transactions"
                    .formatted(
                        createBusinessRecord.allocationRecord().allocation().getAllocationId()),
                HttpMethod.POST,
                createBusinessRecord.authCookie(),
                request)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "Allocation is archived");
  }

  @Test
  void stopAllCards_CancelPhysicalAndVirtual() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final Card rootPhysical1 =
        issueCardForStopAll(
            createBusinessRecord.allocationRecord().allocation(), CardType.PHYSICAL);
    final Card rootPhysical2 =
        issueCardForStopAll(
            createBusinessRecord.allocationRecord().allocation(), CardType.PHYSICAL);
    final Card rootVirtual1 =
        issueCardForStopAll(createBusinessRecord.allocationRecord().allocation(), CardType.VIRTUAL);
    final Card rootVirtual2 =
        issueCardForStopAll(createBusinessRecord.allocationRecord().allocation(), CardType.VIRTUAL);
    final Card childPhysical1 =
        issueCardForStopAll(childAllocation.allocation(), CardType.PHYSICAL);
    final Card childPhysical2 =
        issueCardForStopAll(childAllocation.allocation(), CardType.PHYSICAL);
    final Card childVirtual1 = issueCardForStopAll(childAllocation.allocation(), CardType.VIRTUAL);
    final Card childVirtual2 = issueCardForStopAll(childAllocation.allocation(), CardType.VIRTUAL);

    final StopAllCardsRequest request =
        new StopAllCardsRequest(false, true, StopPhysicalCardsType.CANCEL);
    final StopAllCardsResponse response =
        mockMvcHelper.queryObject(
            "/allocations/%s/cards/stop"
                .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            request,
            StopAllCardsResponse.class);
    assertThat(response.cancelledCards())
        .containsExactlyInAnyOrder(
            rootPhysical1.getId(),
            rootPhysical2.getId(),
            rootVirtual1.getId(),
            rootVirtual2.getId());
    assertThat(response.unlinkedCards()).isEmpty();

    validateCardStatus(
        rootPhysical1, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(
        rootPhysical2, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(
        rootVirtual1, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(
        rootVirtual2, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(childPhysical1, childAllocation.allocation(), CardStatus.INACTIVE);
    validateCardStatus(childPhysical2, childAllocation.allocation(), CardStatus.INACTIVE);
    validateCardStatus(childVirtual1, childAllocation.allocation(), CardStatus.ACTIVE);
    validateCardStatus(childVirtual2, childAllocation.allocation(), CardStatus.ACTIVE);
  }

  private void validateCardStatus(
      @NonNull final Card card,
      @Nullable final Allocation cardAllocation,
      @NonNull final CardStatus expectedStatus) {
    final TypedId<AllocationId> cardAllocationId =
        Optional.ofNullable(cardAllocation).map(Allocation::getId).orElse(null);
    assertThat(cardRepository.findById(card.getId()))
        .isPresent()
        .get()
        .hasFieldOrPropertyWithValue("status", expectedStatus)
        .hasFieldOrPropertyWithValue("allocationId", cardAllocationId);

    final com.stripe.model.issuing.Card stripeCard =
        (com.stripe.model.issuing.Card) stripeMockClient.getCreatedObject(card.getExternalRef());
    if (expectedStatus == CardStatus.CANCELLED) {
      assertThat(stripeCard).isNotNull().hasFieldOrPropertyWithValue("status", "CANCELLED");
    } else {
      assertThat(stripeCard).isNotNull().hasFieldOrPropertyWithValue("status", null);
    }
  }

  @Test
  void stopAllCards_CancelVirtualUnlinkPhysical_ApplyToChildren() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final Card rootPhysical1 =
        issueCardForStopAll(
            createBusinessRecord.allocationRecord().allocation(), CardType.PHYSICAL);
    final Card rootPhysical2 =
        issueCardForStopAll(
            createBusinessRecord.allocationRecord().allocation(), CardType.PHYSICAL);
    final Card rootVirtual1 =
        issueCardForStopAll(createBusinessRecord.allocationRecord().allocation(), CardType.VIRTUAL);
    final Card rootVirtual2 =
        issueCardForStopAll(createBusinessRecord.allocationRecord().allocation(), CardType.VIRTUAL);
    final Card childPhysical1 =
        issueCardForStopAll(childAllocation.allocation(), CardType.PHYSICAL);
    final Card childPhysical2 =
        issueCardForStopAll(childAllocation.allocation(), CardType.PHYSICAL);
    final Card childVirtual1 = issueCardForStopAll(childAllocation.allocation(), CardType.VIRTUAL);
    final Card childVirtual2 = issueCardForStopAll(childAllocation.allocation(), CardType.VIRTUAL);

    final StopAllCardsRequest request =
        new StopAllCardsRequest(true, true, StopPhysicalCardsType.UNLINK);
    final StopAllCardsResponse response =
        mockMvcHelper.queryObject(
            "/allocations/%s/cards/stop"
                .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            request,
            StopAllCardsResponse.class);
    assertThat(response.cancelledCards())
        .containsExactlyInAnyOrder(
            rootVirtual1.getId(),
            rootVirtual2.getId(),
            childVirtual1.getId(),
            childVirtual2.getId());
    assertThat(response.unlinkedCards())
        .containsExactlyInAnyOrder(
            rootPhysical1.getId(),
            rootPhysical2.getId(),
            childPhysical1.getId(),
            childPhysical2.getId());

    validateCardStatus(rootPhysical1, null, CardStatus.INACTIVE);
    validateCardStatus(rootPhysical2, null, CardStatus.INACTIVE);
    validateCardStatus(
        rootVirtual1, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(
        rootVirtual2, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(childPhysical1, null, CardStatus.INACTIVE);
    validateCardStatus(childPhysical2, null, CardStatus.INACTIVE);
    validateCardStatus(childVirtual1, childAllocation.allocation(), CardStatus.CANCELLED);
    validateCardStatus(childVirtual2, childAllocation.allocation(), CardStatus.CANCELLED);
  }

  @Test
  void stopAllCards_CancelVirtualUnlinkPhysical() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final Card rootPhysical1 =
        issueCardForStopAll(
            createBusinessRecord.allocationRecord().allocation(), CardType.PHYSICAL);
    final Card rootPhysical2 =
        issueCardForStopAll(
            createBusinessRecord.allocationRecord().allocation(), CardType.PHYSICAL);
    final Card rootVirtual1 =
        issueCardForStopAll(createBusinessRecord.allocationRecord().allocation(), CardType.VIRTUAL);
    final Card rootVirtual2 =
        issueCardForStopAll(createBusinessRecord.allocationRecord().allocation(), CardType.VIRTUAL);
    final Card childPhysical1 =
        issueCardForStopAll(childAllocation.allocation(), CardType.PHYSICAL);
    final Card childPhysical2 =
        issueCardForStopAll(childAllocation.allocation(), CardType.PHYSICAL);
    final Card childVirtual1 = issueCardForStopAll(childAllocation.allocation(), CardType.VIRTUAL);
    final Card childVirtual2 = issueCardForStopAll(childAllocation.allocation(), CardType.VIRTUAL);

    final StopAllCardsRequest request =
        new StopAllCardsRequest(false, true, StopPhysicalCardsType.UNLINK);
    final StopAllCardsResponse response =
        mockMvcHelper.queryObject(
            "/allocations/%s/cards/stop"
                .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            request,
            StopAllCardsResponse.class);
    assertThat(response.cancelledCards())
        .containsExactlyInAnyOrder(rootVirtual1.getId(), rootVirtual2.getId());
    assertThat(response.unlinkedCards())
        .containsExactlyInAnyOrder(rootPhysical1.getId(), rootPhysical2.getId());

    validateCardStatus(rootPhysical1, null, CardStatus.INACTIVE);
    validateCardStatus(rootPhysical2, null, CardStatus.INACTIVE);
    validateCardStatus(
        rootVirtual1, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(
        rootVirtual2, createBusinessRecord.allocationRecord().allocation(), CardStatus.CANCELLED);
    validateCardStatus(childPhysical1, childAllocation.allocation(), CardStatus.INACTIVE);
    validateCardStatus(childPhysical2, childAllocation.allocation(), CardStatus.INACTIVE);
    validateCardStatus(childVirtual1, childAllocation.allocation(), CardStatus.ACTIVE);
    validateCardStatus(childVirtual2, childAllocation.allocation(), CardStatus.ACTIVE);
  }

  @Test
  void stopAllCards_UserPermissions() {
    final StopAllCardsRequest request =
        new StopAllCardsRequest(false, true, StopPhysicalCardsType.UNLINK);
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvcHelper.query(
                "/allocations/%s/cards/stop"
                    .formatted(createBusinessRecord.allocationRecord().allocation().getId()),
                HttpMethod.PATCH,
                cookie,
                request);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateMockMvcCall(action);
  }

  @SneakyThrows
  @Test
  void createAllocation_success() {
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();

    CreateAllocationRequest request =
        new CreateAllocationRequest(
            testHelper.generateFullName(),
            rootAllocation.getId(),
            new Amount(Currency.USD, BigDecimal.ZERO),
            Collections.singletonList(new CurrencyLimit(Currency.USD, new HashMap<>())),
            Collections.emptySet(),
            Collections.emptySet(),
            false);

    String body = objectMapper.writeValueAsString(request);

    Cookie authCookie = testHelper.login(createBusinessRecord.user());

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
    AllocationRecord allocationRecord = createBusinessRecord.allocationRecord();
    Cookie authCookie = createBusinessRecord.authCookie();

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
    Business business = createBusinessRecord.business();
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();
    testHelper.setCurrentUser(createBusinessRecord.user());
    AllocationRecord parentAllocationRecord =
        testHelper.createAllocation(business.getId(), "", rootAllocation.getId());
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(), "", parentAllocationRecord.allocation().getId());

    Cookie authCookie = createBusinessRecord.authCookie();

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
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount = testHelper.retrieveBusinessBankAccount();
    com.clearspend.capital.common.data.model.Amount amount =
        new com.clearspend.capital.common.data.model.Amount(
            business.getCurrency(), BigDecimal.valueOf(100));
    AdjustmentAndHoldRecord deposit =
        testHelper.transactBankAccount(
            businessBankAccount,
            BankAccountTransactType.DEPOSIT,
            createBusinessRecord.user(),
            amount.getAmount(),
            false);
    final Allocation rootAllocation =
        serviceHelper
            .allocationService()
            .getRootAllocation(testHelper.retrieveBusiness().getId())
            .allocation();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(business.getId(), "", rootAllocation.getId());
    Card card =
        testHelper.issueCard(
            business,
            allocationRecord.allocation(),
            testHelper.createUser(business).user(),
            amount.getCurrency(),
            FundingType.INDIVIDUAL,
            CardType.PHYSICAL,
            false);
    Account businessAccount =
        serviceHelper.allocationService().getRootAllocation(business.getId()).account();
    AccountReallocateFundsRecord reallocateFundsRecord =
        serviceHelper
            .accountService()
            .reallocateFunds(businessAccount.getId(), allocationRecord.account().getId(), amount);

    AllocationFundCardRequest request =
        new AllocationFundCardRequest(
            allocationRecord.account().getId(),
            card.getId(),
            AllocationReallocationType.ALLOCATION_TO_CARD,
            Amount.of(amount));

    String body = objectMapper.writeValueAsString(request);
    Cookie authCookie = createBusinessRecord.authCookie();

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
    testHelper.setCurrentUser(createBusinessRecord.user());

    Business business = createBusinessRecord.business();
    BusinessBankAccount businessBankAccount = testHelper.retrieveBusinessBankAccount();
    com.clearspend.capital.common.data.model.Amount amount =
        new com.clearspend.capital.common.data.model.Amount(
            business.getCurrency(), BigDecimal.valueOf(100));
    testHelper.transactBankAccount(
        businessBankAccount,
        BankAccountTransactType.DEPOSIT,
        createBusinessRecord.user(),
        amount.getAmount(),
        false);
    final Allocation rootAllocation =
        serviceHelper.allocationService().getRootAllocation(business.getId()).allocation();
    CreateAllocationRequest request =
        new CreateAllocationRequest(
            testHelper.generateFullName(),
            rootAllocation.getId(),
            new Amount(Currency.USD, BigDecimal.valueOf(10)),
            Collections.singletonList(new CurrencyLimit(Currency.USD, new HashMap<>())),
            Collections.emptySet(),
            Collections.emptySet(),
            false);

    String body = objectMapper.writeValueAsString(request);
    Cookie authCookie = createBusinessRecord.authCookie();

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
    Business business = createBusinessRecord.business();
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User firstALlocationOwner = testHelper.createUser(business).user();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(business.getId(), faker.name().name(), rootAllocation.getId());

    UpdateAllocationRequest updateAllocationRequest = new UpdateAllocationRequest();
    updateAllocationRequest.setName("Changed name");
    updateAllocationRequest.setLimits(
        Collections.singletonList(
            new CurrencyLimit(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(LimitPeriod.DAILY, new Limit(BigDecimal.TEN, BigDecimal.ZERO))))));
    updateAllocationRequest.setDisabledMccGroups(EnumSet.allOf(MccGroup.class));
    updateAllocationRequest.setDisabledPaymentTypes(
        Collections.singleton(PaymentType.MANUAL_ENTRY));
    updateAllocationRequest.setDisableForeign(true);

    Cookie authCookie = createBusinessRecord.authCookie();

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
    assertThat(allocationDetailsResponse.getLimits())
        .isEqualTo(updateAllocationRequest.getLimits());
    assertThat(allocationDetailsResponse.getDisabledMccGroups())
        .isEqualTo(updateAllocationRequest.getDisabledMccGroups());
    assertThat(allocationDetailsResponse.getDisabledPaymentTypes())
        .isEqualTo(updateAllocationRequest.getDisabledPaymentTypes());
    assertThat(allocationDetailsResponse.getDisableForeign()).isTrue();
  }
}
