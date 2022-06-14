package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.common.advice.GlobalControllerExceptionHandler.ControllerError;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.allocation.AllocationFundsManagerResponse;
import com.clearspend.capital.controller.type.business.BusinessSettings;
import com.clearspend.capital.controller.type.card.CardAllocationDetails;
import com.clearspend.capital.controller.type.card.CardAllocationSpendControls;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.EphemeralKeyRequest;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.IssueCardResponse;
import com.clearspend.capital.controller.type.card.RevealCardRequest;
import com.clearspend.capital.controller.type.card.RevealCardResponse;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.card.UpdateCardSpendControlsRequest;
import com.clearspend.capital.controller.type.card.UpdateCardStatusRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.CardAllocation;
import com.clearspend.capital.data.model.ReplacementReason;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.card.CardholderType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardAllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.testutils.assertions.AssertCardAllocationDetailsResponse;
import com.clearspend.capital.testutils.assertions.AssertCardAllocationDetailsResponse.LimitsToAssert;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.util.function.ThrowableFunctions.ThrowingFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.github.javafaker.Faker;
import com.stripe.model.issuing.Cardholder;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class CardControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvcHelper mockMvcHelper;
  private final AssertCardAllocationDetailsResponse assertCardAllocationDetailsResponse;
  private final MockMvc mockMvc;
  private final EntityManager entityManager;
  private final CardRepository cardRepository;
  private final CardAllocationRepository cardAllocationRepository;
  private final BusinessRepository businessRepository;
  private final UserRepository userRepository;
  private final AllocationRepository allocationRepository;
  private final TransactionLimitRepository transactionLimitRepository;
  private final PermissionValidationHelper permissionValidationHelper;
  private final StripeMockClient stripeMockClient;

  private final Faker faker = new Faker();

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private TypedId<UserId> userId;
  private Cookie userCookie;
  private Card card;
  private final TwilioServiceMock twilioServiceMock;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    business = createBusinessRecord.business();
    userId = createBusinessRecord.user().getId();
    userCookie = createBusinessRecord.authCookie();
    testHelper.setCurrentUser(createBusinessRecord.user());
    card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            entityManager.getReference(User.class, userId),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    twilioServiceMock.setLastCardUnlinkedEmail(null);
  }

  @AfterEach
  public void cleanup() {
    stripeMockClient.reset();
  }

  @SneakyThrows
  @Test
  void createCard() {
    TypedId<AllocationId> allocationId =
        testHelper.createAllocationMvc(
            userId,
            faker.name().name(),
            createBusinessRecord.allocationRecord().allocation().getId());

    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limitMap =
        Map.of(
            Currency.USD,
            Map.of(
                LimitType.PURCHASE,
                Map.of(LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)));

    final CardAllocationSpendControls controls = new CardAllocationSpendControls(allocationId);
    controls.setLimits(CurrencyLimit.ofMap(limitMap));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY));
    controls.setDisableForeign(false);

    IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.VIRTUAL, CardType.PHYSICAL),
            userId,
            Currency.USD,
            true,
            List.of(controls));
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    entityManager.flush();

    List<IssueCardResponse> issueCardResponse =
        mockMvcHelper.queryList(
            "/cards", HttpMethod.POST, userCookie, issueCardRequest, new TypeReference<>() {});

    assertThat(issueCardResponse).hasSize(2);
    final CardAllocation cardAllocation1 =
        testAndGetCardAllocations(issueCardResponse.get(0).getCardId(), List.of(allocationId))
            .get(allocationId);
    final CardAllocation cardAllocation2 =
        testAndGetCardAllocations(issueCardResponse.get(1).getCardId(), List.of(allocationId))
            .get(allocationId);

    testTransactionLimit(cardAllocation1, controls);
    testTransactionLimit(cardAllocation2, controls);

    final Card dbCard1 =
        cardRepository.findById(issueCardResponse.get(0).getCardId()).orElseThrow();
    assertThat(dbCard1).hasFieldOrPropertyWithValue("allocationId", allocationId);
    final Card dbCard2 =
        cardRepository.findById(issueCardResponse.get(1).getCardId()).orElseThrow();
    assertThat(dbCard2).hasFieldOrPropertyWithValue("allocationId", allocationId);
  }

  private void testTransactionLimit(
      final CardAllocation cardAllocation, final CardAllocationSpendControls request) {
    final TransactionLimit allocationLimit =
        transactionLimitRepository
            .findByBusinessIdAndTypeAndOwnerId(
                createBusinessRecord.business().getId(),
                TransactionLimitType.ALLOCATION,
                cardAllocation.getAllocationId().toUuid())
            .orElseThrow();
    final TransactionLimit cardLimit =
        transactionLimitRepository
            .findByBusinessIdAndTypeAndOwnerId(
                createBusinessRecord.business().getId(),
                TransactionLimitType.CARD,
                cardAllocation.getId().toUuid())
            .orElseThrow();

    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Optional.ofNullable(request.getLimits())
            .map(CurrencyLimit::toMap)
            .orElse(allocationLimit.getLimits());
    final Set<MccGroup> disabledMccGroups =
        Optional.ofNullable(request.getDisabledMccGroups())
            .orElse(allocationLimit.getDisabledMccGroups());
    final Set<PaymentType> disabledPaymentTypes =
        Optional.ofNullable(request.getDisabledPaymentTypes())
            .orElse(allocationLimit.getDisabledPaymentTypes());
    final Boolean disableForeign =
        Optional.ofNullable(request.getDisableForeign())
            .orElse(allocationLimit.getDisableForeign());

    assertThat(cardLimit)
        .hasFieldOrPropertyWithValue("limits", limits)
        .hasFieldOrPropertyWithValue("disabledMccGroups", disabledMccGroups)
        .hasFieldOrPropertyWithValue("disabledPaymentTypes", disabledPaymentTypes)
        .hasFieldOrPropertyWithValue("disableForeign", disableForeign);
  }

  private Map<TypedId<AllocationId>, CardAllocation> testAndGetCardAllocations(
      final TypedId<CardId> cardId, final List<TypedId<AllocationId>> allocationIds) {
    final List<CardAllocation> cardAllocations = cardAllocationRepository.findAllByCardId(cardId);
    assertThat(cardAllocations)
        .filteredOn(ca -> allocationIds.contains(ca.getAllocationId()))
        .hasSize(allocationIds.size());
    return cardAllocations.stream()
        .collect(Collectors.toMap(CardAllocation::getAllocationId, Function.identity()));
  }

  @Test
  void createCard_MultipleAllocations() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());

    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limitMap =
        Map.of(
            Currency.USD,
            Map.of(
                LimitType.PURCHASE,
                Map.of(LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)));

    final CardAllocationSpendControls rootControls =
        new CardAllocationSpendControls(
            createBusinessRecord.allocationRecord().allocation().getId());
    rootControls.setLimits(CurrencyLimit.ofMap(limitMap));
    rootControls.setDisabledMccGroups(Set.of());
    rootControls.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY));
    rootControls.setDisableForeign(false);
    final CardAllocationSpendControls childControls =
        new CardAllocationSpendControls(childAllocation.allocation().getId());

    IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.PHYSICAL),
            userId,
            Currency.USD,
            true,
            List.of(rootControls, childControls));
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    entityManager.flush();

    List<IssueCardResponse> issueCardResponse =
        mockMvcHelper.queryList(
            "/cards", HttpMethod.POST, userCookie, issueCardRequest, new TypeReference<>() {});

    assertThat(issueCardResponse).hasSize(1);
    final Map<TypedId<AllocationId>, CardAllocation> cardAllocations =
        testAndGetCardAllocations(
            issueCardResponse.get(0).getCardId(),
            List.of(
                createBusinessRecord.allocationRecord().allocation().getId(),
                childAllocation.allocation().getId()));

    final Card dbCard = cardRepository.findById(issueCardResponse.get(0).getCardId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId());

    testTransactionLimit(
        cardAllocations.get(createBusinessRecord.allocationRecord().allocation().getId()),
        rootControls);
    testTransactionLimit(cardAllocations.get(childAllocation.allocation().getId()), childControls);
  }

  @Test
  void issueCard_BusinessCard_SpecialUserPermissions() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());

    final IssueCardRequest request =
        new IssueCardRequest(
            Set.of(CardType.VIRTUAL),
            createBusinessRecord.user().getId(),
            Currency.USD,
            true,
            List.of(CardAllocationSpendControls.of(childAllocation.allocation())));
    request.setCardholderType(CardholderType.BUSINESS);

    final ThrowingFunction<Cookie, ResultActions> action =
        cookie -> mockMvcHelper.query("/cards", HttpMethod.POST, cookie, request);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation.allocation())
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  @SneakyThrows
  void issueCard_BusinessCardholder_OwnerMustBeAdminOnRoot() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final User adminOnRoot =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();
    final User adminOnChild =
        testHelper
            .createUserWithRole(childAllocation.allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();

    final Function<TypedId<UserId>, IssueCardRequest> requestCreator =
        userId -> {
          final IssueCardRequest request =
              new IssueCardRequest(
                  Set.of(CardType.VIRTUAL),
                  userId,
                  Currency.USD,
                  true,
                  List.of(CardAllocationSpendControls.of(childAllocation.allocation())));
          request.setCardholderType(CardholderType.BUSINESS);
          return request;
        };

    entityManager.flush();

    final JavaType responseType =
        objectMapper.getTypeFactory().constructParametricType(List.class, IssueCardResponse.class);
    mockMvcHelper
        .query(
            "/cards",
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            requestCreator.apply(adminOnRoot.getId()))
        .andExpect(status().isOk());
    final String response =
        mockMvcHelper
            .query(
                "/cards",
                HttpMethod.POST,
                createBusinessRecord.authCookie(),
                requestCreator.apply(adminOnChild.getId()))
            .andExpect(status().is(400))
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error)
        .hasFieldOrPropertyWithValue(
            "message", "Business Card owner is not Admin on root allocation");
  }

  @Test
  void issueCard_BusinessCardholder() {
    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(
        CurrencyLimit.ofMap(
            Map.of(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(
                        LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)))));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY));
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.VIRTUAL),
            createBusinessRecord.user().getId(),
            Currency.USD,
            true,
            List.of(controls));
    issueCardRequest.setCardholderType(CardholderType.BUSINESS);

    entityManager.flush();

    final JavaType responseType =
        objectMapper.getTypeFactory().constructParametricType(List.class, IssueCardResponse.class);
    final List<IssueCardResponse> response =
        mockMvcHelper.queryObject(
            "/cards",
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            issueCardRequest,
            responseType);
    assertThat(response).hasSize(1);

    final Card card = cardRepository.findById(response.get(0).getCardId()).orElseThrow();
    assertThat(card)
        .hasFieldOrPropertyWithValue("cardholderType", CardholderType.BUSINESS)
        .hasFieldOrPropertyWithValue("userId", createBusinessRecord.user().getId());

    final Business dbBusiness =
        businessRepository.findById(createBusinessRecord.business().getId()).orElseThrow();
    assertThat(dbBusiness.getCardholderExternalRef()).isNotNull();

    final Cardholder cardholder =
        (Cardholder) stripeMockClient.getCreatedObject(business.getCardholderExternalRef());
    assertThat(cardholder).isNotNull();

    final com.stripe.model.issuing.Card stripeCard =
        (com.stripe.model.issuing.Card) stripeMockClient.getCreatedObject(card.getExternalRef());
    assertThat(stripeCard).isNotNull();
    assertThat(stripeCard.getCardholder())
        .hasFieldOrPropertyWithValue("id", dbBusiness.getCardholderExternalRef());
  }

  @Test
  @SneakyThrows
  void issueCard_ReplacementFor() {
    final Card firstCard =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    com.clearspend.capital.controller.type.card.Card firstCardCancelled =
        mockMvcHelper.queryObject(
            "/users/cards/%s/cancel".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.LOST),
            com.clearspend.capital.controller.type.card.Card.class);

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(
        CurrencyLimit.ofMap(
            Map.of(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(
                        LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)))));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY));
    controls.setDisableForeign(false);

    final String replacementFor = firstCard.getExternalRef();
    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.VIRTUAL),
            createBusinessRecord.user().getId(),
            Currency.USD,
            true,
            List.of(controls));
    issueCardRequest.setReplacementFor(replacementFor);
    issueCardRequest.setReplacementReason(ReplacementReason.LOST);

    entityManager.flush();

    final JavaType responseType =
        objectMapper.getTypeFactory().constructParametricType(List.class, IssueCardResponse.class);
    final List<IssueCardResponse> response =
        mockMvcHelper.queryObject(
            "/cards",
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            issueCardRequest,
            responseType);
    assertThat(response).hasSize(1);

    final Card card = cardRepository.findById(response.get(0).getCardId()).orElseThrow();
    assertThat(card.getReplacement())
        .hasFieldOrPropertyWithValue("cardId", firstCard.getId())
        .hasFieldOrPropertyWithValue("reason", ReplacementReason.LOST);

    final com.stripe.model.issuing.Card stripeCard =
        (com.stripe.model.issuing.Card) stripeMockClient.getCreatedObject(card.getExternalRef());
    assertThat(stripeCard)
        .hasFieldOrPropertyWithValue("replacementFor", replacementFor)
        .hasFieldOrPropertyWithValue("replacementReason", ReplacementReason.LOST.getValue());
  }

  @Test
  @SneakyThrows
  void issueCard_AllocationIsArchived() {
    final AllocationRecord allocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getBusinessId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final CreateUpdateUserRecord employeeRecord =
        testHelper.createUserWithRole(allocation.allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    allocation.allocation().setArchived(true);
    allocationRepository.saveAndFlush(allocation.allocation());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(allocation.allocation());
    controls.setLimits(
        CurrencyLimit.ofMap(
            Map.of(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(
                        LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)))));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY));
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.VIRTUAL, CardType.PHYSICAL),
            employeeRecord.user().getId(),
            Currency.USD,
            true,
            List.of(controls));
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    entityManager.flush();

    final String response =
        mockMvcHelper
            .query("/cards", HttpMethod.POST, createBusinessRecord.authCookie(), issueCardRequest)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "Allocation is archived");
  }

  @Test
  @SneakyThrows
  void issueCard_UserIsArchived() {
    final CreateUpdateUserRecord employeeRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    employeeRecord.user().setArchived(true);
    userRepository.save(employeeRecord.user());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(
        CurrencyLimit.ofMap(
            Map.of(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(
                        LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)))));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY));
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.VIRTUAL, CardType.PHYSICAL),
            employeeRecord.user().getId(),
            Currency.USD,
            true,
            List.of(controls));
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    entityManager.flush();

    final String response =
        mockMvcHelper
            .query("/cards", HttpMethod.POST, userCookie, issueCardRequest)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "User has been archived");
  }

  @SneakyThrows
  @Test
  void getUserCard() {
    CardDetailsResponse cardDetailsResponse =
        mockMvcHelper.queryObject(
            "/cards/" + card.getId().toString(),
            HttpMethod.GET,
            userCookie,
            CardDetailsResponse.class);

    assertThat(cardDetailsResponse.getCard()).isNotNull();
    assertThat(cardDetailsResponse.getCard().isActivated()).isFalse();
    assertThat(cardDetailsResponse.getCard().getCardNumber()).isNull();

    assertThat(cardDetailsResponse.getAvailableBalance()).isNotNull();
    assertThat(cardDetailsResponse.getAvailableBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLedgerBalance()).isNotNull();
    assertThat(cardDetailsResponse.getLedgerBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getAllocationSpendControls()).hasSize(1);
    assertCardAllocationDetailsResponse.doAssert(
        createBusinessRecord.allocationRecord().allocation().getId(),
        cardDetailsResponse.getAllocationSpendControls().stream().findFirst().orElseThrow());
  }

  @Test
  @SneakyThrows
  void updateCardSpendControls() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Map.of(
            Currency.USD,
            Map.of(LimitType.ACH_DEPOSIT, Map.of(LimitPeriod.DAILY, BigDecimal.ZERO)));
    final Set<MccGroup> disabledCategories = Set.of(MccGroup.CHILD_CARE, MccGroup.FOOD_BEVERAGE);
    final Set<PaymentType> disabledPaymentTypes =
        Set.of(PaymentType.ONLINE, PaymentType.MANUAL_ENTRY);
    final boolean disableForeign = false;
    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(limits));
    controls.setDisabledMccGroups(disabledCategories);
    controls.setDisabledPaymentTypes(disabledPaymentTypes);
    controls.setDisableForeign(disableForeign);

    final UpdateCardSpendControlsRequest request =
        new UpdateCardSpendControlsRequest(List.of(controls));

    final String content =
        mockMvc
            .perform(
                patch("/cards/%s/controls".formatted(card.getId()))
                    .cookie(createBusinessRecord.authCookie())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final CardDetailsResponse response = objectMapper.readValue(content, CardDetailsResponse.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue(
            "card", new com.clearspend.capital.controller.type.card.Card(card));
    assertThat(response.getAllocationSpendControls()).hasSize(1);

    final LimitsToAssert limitsToAssert =
        new LimitsToAssert(
            CurrencyLimit.ofMap(limits), disabledCategories, disabledPaymentTypes, disableForeign);
    assertCardAllocationDetailsResponse.doAssert(
        limitsToAssert,
        createBusinessRecord.allocationRecord().allocation().getId(),
        response.getAllocationSpendControls().stream().findFirst().orElseThrow());

    final CardAllocation cardAllocation =
        cardAllocationRepository
            .findByCardIdAndAllocationId(
                card.getId(), createBusinessRecord.allocationRecord().allocation().getId())
            .orElseThrow();

    final TransactionLimit transactionLimit =
        transactionLimitRepository
            .findByBusinessIdAndTypeAndOwnerId(
                createBusinessRecord.business().getId(),
                TransactionLimitType.CARD,
                cardAllocation.getId().toUuid())
            .orElseThrow();
    assertThat(transactionLimit)
        .hasFieldOrPropertyWithValue("limits", limits)
        .hasFieldOrPropertyWithValue("disabledMccGroups", disabledCategories)
        .hasFieldOrPropertyWithValue("disabledPaymentTypes", disabledPaymentTypes)
        .hasFieldOrPropertyWithValue("disableForeign", disableForeign);
  }

  @Test
  @SneakyThrows
  void updateCardSpendControls_CancelledCard() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setStatus(CardStatus.CANCELLED);
    card = cardRepository.saveAndFlush(card);
    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Map.of(
            Currency.USD,
            Map.of(LimitType.ACH_DEPOSIT, Map.of(LimitPeriod.DAILY, BigDecimal.ZERO)));
    final Set<MccGroup> disabledCategories = Set.of(MccGroup.CHILD_CARE, MccGroup.FOOD_BEVERAGE);
    final Set<PaymentType> disabledPaymentTypes =
        Set.of(PaymentType.ONLINE, PaymentType.MANUAL_ENTRY);
    final boolean disableForeign = false;

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(limits));
    controls.setDisabledMccGroups(disabledCategories);
    controls.setDisabledPaymentTypes(disabledPaymentTypes);
    controls.setDisableForeign(disableForeign);

    final UpdateCardSpendControlsRequest request =
        new UpdateCardSpendControlsRequest(List.of(controls));

    mockMvc
        .perform(
            patch("/cards/%s/controls".formatted(card.getId()))
                .cookie(createBusinessRecord.authCookie())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateCardSpendControls_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Map.of(
            Currency.USD,
            Map.of(LimitType.ACH_DEPOSIT, Map.of(LimitPeriod.DAILY, BigDecimal.ZERO)));
    final Set<MccGroup> disabledCategories = Set.of(MccGroup.CHILD_CARE, MccGroup.FOOD_BEVERAGE);
    final Set<PaymentType> disabledPaymentTypes =
        Set.of(PaymentType.ONLINE, PaymentType.MANUAL_ENTRY);
    final boolean disableForeign = false;
    final CardAllocation childCardAllocation =
        cardAllocationRepository.save(
            new CardAllocation(card.getId(), childAllocation.allocation().getId()));
    transactionLimitRepository.save(
        new TransactionLimit(
            createBusinessRecord.business().getId(),
            TransactionLimitType.CARD,
            childCardAllocation.getId().toUuid(),
            limits,
            disabledCategories,
            disabledPaymentTypes,
            disableForeign));

    final User managerOnChild =
        testHelper
            .createUserWithRole(childAllocation.allocation(), DefaultRoles.ALLOCATION_MANAGER)
            .user();

    entityManager.flush();

    final List<CardAllocationSpendControls> allControls =
        Stream.of(
                createBusinessRecord.allocationRecord().allocation(), childAllocation.allocation())
            .map(
                allocation -> {
                  final CardAllocationSpendControls controls =
                      CardAllocationSpendControls.of(allocation);
                  controls.setLimits(CurrencyLimit.ofMap(limits));
                  controls.setDisabledMccGroups(disabledCategories);
                  controls.setDisabledPaymentTypes(disabledPaymentTypes);
                  controls.setDisableForeign(disableForeign);
                  return controls;
                })
            .toList();

    final UpdateCardSpendControlsRequest request = new UpdateCardSpendControlsRequest(allControls);

    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvc.perform(
                patch("/cards/%s/controls".formatted(card.getId()))
                    .cookie(cookie)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)));

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .denyUser(managerOnChild)
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  @SneakyThrows
  void updateCardLimits() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    // given
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            testHelper.createUser(business).user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(
        CurrencyLimit.ofMap(
            Map.of(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(
                        LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)))));
    controls.setDisabledMccGroups(Set.of(MccGroup.CHILD_CARE));
    controls.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY, PaymentType.ONLINE));
    controls.setDisableForeign(true);
    final UpdateCardSpendControlsRequest request =
        new UpdateCardSpendControlsRequest(List.of(controls));

    CardDetailsResponse cardDetailsResponse =
        mockMvcHelper.queryObject(
            "/cards/%s/controls".formatted(card.getId()),
            HttpMethod.PATCH,
            userCookie,
            request,
            CardDetailsResponse.class);

    assertThat(cardDetailsResponse.getAllocationSpendControls()).hasSize(1);

    assertCardAllocationDetailsResponse.doAssert(
        LimitsToAssert.fromUpdateCardRequest(controls),
        createBusinessRecord.allocationRecord().allocation().getId(),
        cardDetailsResponse.getAllocationSpendControls().stream().findFirst().orElseThrow());
  }

  @SneakyThrows
  @Test
  void revealCard() {
    RevealCardRequest revealCardRequest = new RevealCardRequest(card.getId(), "test-nonce");
    RevealCardResponse revealCardResponse =
        mockMvcHelper.queryObject(
            "/cards/reveal",
            HttpMethod.POST,
            userCookie,
            revealCardRequest,
            RevealCardResponse.class);
    assertThat(revealCardResponse.getExternalRef()).isNotNull();
    assertThat(revealCardResponse.getEphemeralKey()).isNotNull();
  }

  @SneakyThrows
  @Test
  void ephemeralKey() {
    EphemeralKeyRequest ephemeralKeyRequest = new EphemeralKeyRequest(card.getId(), "2020-03-02");
    String ephemeralKeyResponse =
        mockMvcHelper.queryObject(
            "/cards/ephemeral-key", HttpMethod.POST, userCookie, ephemeralKeyRequest, String.class);
    assertThat(ephemeralKeyResponse).isEqualTo("true");
  }

  @Test
  void physicalCardsIssuanceLimit() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);

    BusinessSettings businessSettings =
        mockMvcHelper.queryObject(
            "/businesses/business-settings", HttpMethod.GET, userCookie, BusinessSettings.class);

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of(PaymentType.ONLINE));
    controls.setDisableForeign(false);

    IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.PHYSICAL), userId, Currency.USD, true, List.of(controls));
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    for (int i = businessSettings.getIssuedPhysicalCardsTotal();
        i < businessSettings.getIssuedPhysicalCardsLimit();
        i++) {
      mockMvcHelper.queryList(
          "/cards", HttpMethod.POST, userCookie, issueCardRequest, new TypeReference<>() {});
    }

    assertThatThrownBy(
            () ->
                mockMvcHelper.queryList(
                    "/cards",
                    HttpMethod.POST,
                    userCookie,
                    issueCardRequest,
                    new TypeReference<>() {}))
        .isInstanceOf(AssertionError.class)
        .hasMessage("Status expected:<200> but was:<400>");
  }

  @Test
  void updateCard_CardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setDisableForeign(true);

    final UpdateCardSpendControlsRequest request =
        new UpdateCardSpendControlsRequest(List.of(controls));
    final CardDetailsResponse response =
        mockMvcHelper.queryObject(
            "/cards/%s/controls".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            request,
            CardDetailsResponse.class);
    assertThat(response.getCard())
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("cardId", card.getId());
    assertThat(response.getAllocationSpendControls()).hasSize(1);
    final LimitsToAssert limitsToAssert =
        new LimitsToAssert(
            List.of(new CurrencyLimit(Currency.USD, new HashMap<>())), Set.of(), Set.of(), true);
    assertCardAllocationDetailsResponse.doAssert(
        limitsToAssert,
        createBusinessRecord.allocationRecord().allocation().getId(),
        response.getAllocationSpendControls().stream().findFirst().orElseThrow());
  }

  @Test
  void getCard_CardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);
    final CardDetailsResponse result =
        mockMvcHelper.queryObject(
            "/cards/%s".formatted(card.getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            CardDetailsResponse.class);

    assertThat(result.getCard())
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("cardId", card.getId());
    assertThat(result)
        .hasFieldOrPropertyWithValue("ledgerBalance", null)
        .hasFieldOrPropertyWithValue("availableBalance", null)
        .hasFieldOrPropertyWithValue("linkedAllocationId", null)
        .hasFieldOrPropertyWithValue("linkedAllocationName", null);
  }

  @Test
  void getCard_Unlinked_NoCardAllocations() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    final CardAllocation cardAllocation =
        cardAllocationRepository
            .findByCardIdAndAllocationId(card.getId(), card.getAllocationId())
            .orElseThrow();
    final TransactionLimit transactionLimit =
        transactionLimitRepository
            .findByBusinessIdAndTypeAndOwnerId(
                createBusinessRecord.business().getId(),
                TransactionLimitType.CARD,
                cardAllocation.getId().toUuid())
            .orElseThrow();
    transactionLimitRepository.delete(transactionLimit);
    cardAllocationRepository.delete(cardAllocation);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.save(card);

    entityManager.flush();

    final CardDetailsResponse result =
        mockMvcHelper.queryObject(
            "/cards/%s".formatted(card.getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            CardDetailsResponse.class);

    assertThat(result.getCard())
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("status", CardStatus.ACTIVE);
    assertThat(result)
        .hasFieldOrPropertyWithValue("ledgerBalance", null)
        .hasFieldOrPropertyWithValue("availableBalance", null)
        .hasFieldOrPropertyWithValue("linkedAllocationId", null)
        .hasFieldOrPropertyWithValue("linkedAllocationName", null)
        .hasFieldOrPropertyWithValue("allocationSpendControls", List.of());
  }

  @Test
  void search_OneCardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);

    final SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 100));
    final JavaType responseType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(PagedData.class, SearchCardData.class);
    final PagedData<SearchCardData> result =
        mockMvcHelper.queryObject(
            "/cards/search",
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            request,
            responseType);
    assertThat(result).hasFieldOrPropertyWithValue("totalElements", 2L);
    final List<SearchCardData> unlinkedCards =
        result.getContent().stream()
            .filter(data -> data.getAllocation() == null)
            .collect(Collectors.toList());
    assertThat(unlinkedCards).hasSize(1);
    assertThat(unlinkedCards.get(0)).hasFieldOrPropertyWithValue("cardId", card.getId());
  }

  @Test
  void addAllocationsToCard() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final CardAllocationSpendControls controls1 =
        new CardAllocationSpendControls(
            createBusinessRecord.allocationRecord().allocation().getId());
    controls1.setDisabledPaymentTypes(Set.of(PaymentType.ONLINE));
    final CardAllocationSpendControls controls2 =
        new CardAllocationSpendControls(childAllocation.allocation().getId());
    controls2.setDisabledPaymentTypes(Set.of(PaymentType.ONLINE));

    final CardDetailsResponse response =
        mockMvcHelper.queryObject(
            "/cards/%s/allocations".formatted(card.getId()),
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            List.of(controls1, controls2),
            CardDetailsResponse.class);

    assertThat(response.getCard()).hasFieldOrPropertyWithValue("cardId", card.getId());
    assertThat(response.getAllocationSpendControls()).hasSize(2);
    final List<CardAllocationSpendControls> allowedAllocationsAndLimits =
        response.getAllocationSpendControls().stream()
            .sorted(
                (a, b) ->
                    a.getAllocationId()
                            .equals(createBusinessRecord.allocationRecord().allocation().getId())
                        ? -1
                        : 1)
            .toList();
    assertThat(allowedAllocationsAndLimits.get(0))
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("disabledPaymentTypes", Set.of());
    assertThat(allowedAllocationsAndLimits.get(1))
        .hasFieldOrPropertyWithValue("allocationId", childAllocation.allocation().getId())
        .hasFieldOrPropertyWithValue("disabledPaymentTypes", Set.of(PaymentType.ONLINE));
  }

  @Test
  void addAllocationsToCard_UserPermissions() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final AllocationRecord grandchildAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(), "Child", childAllocation.allocation().getId());
    final User managerOnGrandchild =
        testHelper
            .createUserWithRole(grandchildAllocation.allocation(), DefaultRoles.ALLOCATION_MANAGER)
            .user();
    final CardAllocationSpendControls controls =
        new CardAllocationSpendControls(childAllocation.allocation().getId());
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvcHelper.query(
                "/cards/%s/allocations".formatted(card.getId()),
                HttpMethod.POST,
                cookie,
                List.of(controls));

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_MANAGER, DefaultRoles.ALLOCATION_ADMIN))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .denyUser(managerOnGrandchild)
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  void removeAllocationsFromCard() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final CardAllocation childCardAllocation =
        cardAllocationRepository.save(
            new CardAllocation(card.getId(), childAllocation.allocation().getId()));
    final TransactionLimit transactionLimit =
        transactionLimitRepository.save(
            new TransactionLimit(
                createBusinessRecord.business().getId(),
                TransactionLimitType.CARD,
                childCardAllocation.getId().toUuid(),
                Map.of(),
                Set.of(),
                Set.of(),
                false));
    final CardAllocationDetails allocations =
        new CardAllocationDetails(childAllocation.allocation().getId());

    final CardDetailsResponse response =
        mockMvcHelper.queryObject(
            "/cards/%s/allocations".formatted(card.getId()),
            HttpMethod.DELETE,
            createBusinessRecord.authCookie(),
            List.of(allocations),
            CardDetailsResponse.class);

    assertThat(response.getCard()).hasFieldOrPropertyWithValue("cardId", card.getId());
    assertThat(response)
        .hasFieldOrPropertyWithValue(
            "linkedAllocationId", createBusinessRecord.allocationRecord().allocation().getId());
    assertThat(response.getAllocationSpendControls()).hasSize(1);
    assertThat(response.getAllocationSpendControls().get(0))
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId());

    assertThat(cardAllocationRepository.findById(childCardAllocation.getId())).isEmpty();
    assertThat(transactionLimitRepository.findById(transactionLimit.getId())).isEmpty();

    assertThat(twilioServiceMock).hasFieldOrPropertyWithValue("lastCardUnlinkedEmail", null);
  }

  @Test
  void removeAllocationsFromCard_RemovingLinkedAllocation() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final CardAllocation childCardAllocation =
        cardAllocationRepository.save(
            new CardAllocation(card.getId(), childAllocation.allocation().getId()));
    final TransactionLimit transactionLimit =
        transactionLimitRepository.save(
            new TransactionLimit(
                createBusinessRecord.business().getId(),
                TransactionLimitType.CARD,
                childCardAllocation.getId().toUuid(),
                Map.of(),
                Set.of(),
                Set.of(),
                false));
    final CardAllocationDetails allocations =
        new CardAllocationDetails(createBusinessRecord.allocationRecord().allocation().getId());

    final CardAllocation rootCardAllocation =
        cardAllocationRepository
            .findByCardIdAndAllocationId(
                card.getId(), createBusinessRecord.allocationRecord().allocation().getId())
            .orElseThrow();
    final TransactionLimit rootTransactionLimit =
        transactionLimitRepository
            .findByBusinessIdAndTypeAndOwnerId(
                createBusinessRecord.business().getId(),
                TransactionLimitType.CARD,
                rootCardAllocation.getId().toUuid())
            .orElseThrow();

    final CardDetailsResponse response =
        mockMvcHelper.queryObject(
            "/cards/%s/allocations".formatted(card.getId()),
            HttpMethod.DELETE,
            createBusinessRecord.authCookie(),
            List.of(allocations),
            CardDetailsResponse.class);

    assertThat(response.getCard()).hasFieldOrPropertyWithValue("cardId", card.getId());
    assertThat(response).hasFieldOrPropertyWithValue("linkedAllocationId", null);
    assertThat(response.getAllocationSpendControls()).hasSize(1);
    assertThat(response.getAllocationSpendControls().get(0))
        .hasFieldOrPropertyWithValue("allocationId", childAllocation.allocation().getId());

    assertThat(cardAllocationRepository.findById(rootCardAllocation.getId())).isEmpty();
    assertThat(transactionLimitRepository.findById(rootTransactionLimit.getId())).isEmpty();

    assertThat(twilioServiceMock)
        .hasFieldOrPropertyWithValue(
            "lastCardUnlinkedEmail", createBusinessRecord.user().getEmail().getEncrypted());
  }

  @Test
  void removeAllocationsFromCard_UserPermissions() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final AllocationRecord grandchildAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(), "Child", childAllocation.allocation().getId());
    final User managerOnGrandchild =
        testHelper
            .createUserWithRole(grandchildAllocation.allocation(), DefaultRoles.ALLOCATION_MANAGER)
            .user();
    final CardAllocationDetails allocations =
        new CardAllocationDetails(childAllocation.allocation().getId());
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvcHelper.query(
                "/cards/%s/allocations".formatted(card.getId()),
                HttpMethod.DELETE,
                cookie,
                List.of(allocations));

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_MANAGER, DefaultRoles.ALLOCATION_ADMIN))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .denyUser(managerOnGrandchild)
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  void reassignCardOwner() {
    final User admin =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setCardholderType(CardholderType.BUSINESS);
    cardRepository.saveAndFlush(card);

    final com.clearspend.capital.controller.type.card.Card response =
        mockMvcHelper.queryObject(
            "/cards/%s/reassign/%s".formatted(card.getId(), admin.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            com.clearspend.capital.controller.type.card.Card.class);

    assertThat(response).hasFieldOrPropertyWithValue("userId", admin.getId());

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard).hasFieldOrPropertyWithValue("userId", admin.getId());
  }

  @Test
  @SneakyThrows
  void reassignCardOwner_NotBusinessCard() {
    final User admin =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    final String response =
        mockMvcHelper
            .query(
                "/cards/%s/reassign/%s".formatted(card.getId(), admin.getId()),
                HttpMethod.PATCH,
                createBusinessRecord.authCookie())
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error)
        .hasFieldOrPropertyWithValue("message", "Owner can only be reassigned for Business Cards");
  }

  @Test
  @SneakyThrows
  void reassignCardOwner_InvalidNewOwner() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final User adminOnChild =
        testHelper
            .createUserWithRole(childAllocation.allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();
    final User manager =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_MANAGER)
            .user();
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setCardholderType(CardholderType.BUSINESS);
    cardRepository.saveAndFlush(card);

    final ThrowingFunction<TypedId<UserId>, ControllerError> doRequest =
        userId -> {
          final String response =
              mockMvcHelper
                  .query(
                      "/cards/%s/reassign/%s".formatted(card.getId(), userId),
                      HttpMethod.PATCH,
                      createBusinessRecord.authCookie())
                  .andExpect(status().isBadRequest())
                  .andReturn()
                  .getResponse()
                  .getContentAsString();
          return objectMapper.readValue(response, ControllerError.class);
        };

    final ControllerError adminOnChildError = doRequest.apply(adminOnChild.getId());
    assertThat(adminOnChildError)
        .hasFieldOrPropertyWithValue(
            "message", "Business Card owner is not Admin on root allocation");
    final ControllerError managerError = doRequest.apply(manager.getId());
    assertThat(managerError)
        .hasFieldOrPropertyWithValue(
            "message", "Business Card owner is not Admin on root allocation");
  }

  @Test
  void reassignCardOwner_UserPermissions() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final User admin =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setCardholderType(CardholderType.BUSINESS);
    cardRepository.saveAndFlush(card);

    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvcHelper.query(
                "/cards/%s/reassign/%s".formatted(card.getId(), admin.getId()),
                HttpMethod.PATCH,
                cookie);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(childAllocation.allocation())
        .allowRolesOnRootAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .build()
        .validateMockMvcCall(action);
  }

  @SneakyThrows
  @Test
  void getAllocationManagers() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.init();
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();
    final User rootAllocationOwner = createBusinessRecord.user();
    testHelper.setCurrentUser(rootAllocationOwner);
    User manager = testHelper.createUser(createBusinessRecord.business()).user();
    User employee = testHelper.createUser(createBusinessRecord.business()).user();
    mockMvc
        .perform(
            put("/user-allocation-roles/allocation/%s/user/%s"
                    .formatted(
                        rootAllocation.getId().toUuid().toString(),
                        manager.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .content(DefaultRoles.ALLOCATION_MANAGER)
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();
    mockMvc
        .perform(
            put("/user-allocation-roles/allocation/%s/user/%s"
                    .formatted(
                        rootAllocation.getId().toUuid().toString(),
                        employee.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .content(DefaultRoles.ALLOCATION_EMPLOYEE)
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();
    entityManager.flush();

    MockHttpServletResponse response403 =
        mockMvc
            .perform(
                get("/cards/%s/funds-managers".formatted(card.getId().toUuid().toString()))
                    .contentType("application/json")
                    .cookie(testHelper.login(employee)))
            .andExpect(status().is(403))
            .andReturn()
            .getResponse();
    log.info(response403.getContentAsString());

    // Test that the employee gets back the appropriate data when assigned permissions.
    MockHttpServletResponse response =
        mockMvc
            .perform(
                get("/cards/%s/funds-managers".formatted(card.getId().toUuid().toString()))
                    .contentType("application/json")
                    .cookie(testHelper.login(entityManager.getReference(User.class, userId))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
    AllocationFundsManagerResponse allocationFundsManagerResponse =
        objectMapper.readValue(response.getContentAsString(), AllocationFundsManagerResponse.class);

    // Should be 3, two business owners, plus one manager, and the employee who is not a manager is
    // not returned.
    assertEquals(3, allocationFundsManagerResponse.getUserData().size());
  }
}
