package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.common.advice.GlobalControllerExceptionHandler.ControllerError;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityRequest;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.activity.UpdateAccountActivityRequest;
import com.clearspend.capital.controller.type.card.ActivateCardRequest;
import com.clearspend.capital.controller.type.card.CardAccount;
import com.clearspend.capital.controller.type.card.CardAllocationSpendControls;
import com.clearspend.capital.controller.type.card.CardAndAccount;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.UpdateCardStatusRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.user.CreateUserRequest;
import com.clearspend.capital.controller.type.user.CreateUserResponse;
import com.clearspend.capital.controller.type.user.SearchUserRequest;
import com.clearspend.capital.controller.type.user.UpdateUserRequest;
import com.clearspend.capital.controller.type.user.UpdateUserResponse;
import com.clearspend.capital.controller.type.user.User;
import com.clearspend.capital.controller.type.user.UserPageData;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.CardAllocation;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardAllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.data.repository.ReceiptRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.CoreFusionAuthService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.RolesAndPermissionsService;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.testutils.assertions.AssertCardAllocationDetailsResponse;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.data.TestDataHelper.AccountActivityConfig;
import com.clearspend.capital.testutils.data.TestDataHelper.ReceiptConfig;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.util.function.ThrowableFunctions.ThrowingFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.github.javafaker.Faker;
import com.stripe.param.issuing.CardUpdateParams;
import com.stripe.param.issuing.CardUpdateParams.CancellationReason;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class UserControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mockMvcHelper;
  private final EntityManager entityManager;
  private final AssertCardAllocationDetailsResponse assertCardAllocationDetailsResponse;
  private final TestHelper testHelper;
  private final CardService cardService;
  private final NetworkMessageService networkMessageService;
  private final AccountRepository accountRepository;
  private final TransactionLimitRepository transactionLimitRepository;
  private final UserService userService;
  private final CardRepository cardRepository;
  private final TestDataHelper testDataHelper;
  private final ExpenseCategoryRepository expenseCategoryRepository;
  private final AllocationRepository allocationRepository;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final PermissionValidationHelper permissionValidationHelper;
  private final AccountActivityRepository accountActivityRepo;
  private final ReceiptRepository receiptRepo;
  private final CardAllocationRepository cardAllocationRepository;
  private final StripeMockClient stripeMockClient;
  private final UserRepository userRepository;

  private final Faker faker = new Faker();
  private final CoreFusionAuthService fusionAuthService;
  private final ServiceHelper serviceHelper;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private CreateUpdateUserRecord user;
  private Cookie userCookie;
  private Card card;
  private Card card2;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.init();
    business = createBusinessRecord.business();
    user =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    userCookie = testHelper.login(user.user());
    testHelper.setCurrentUser(createBusinessRecord.user());
    card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    card2 =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    testHelper.setCurrentUser(createBusinessRecord.user());
  }

  @AfterEach
  public void cleanup() {
    stripeMockClient.reset();
  }

  @Test
  @SneakyThrows
  void getCurrentBusinessOwner() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    MockHttpServletResponse response =
        mvc.perform(
                get("/users")
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    User user = objectMapper.readValue(response.getContentAsString(), User.class);
    log.info("\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(user));
  }

  @SneakyThrows
  @Test
  void createUser() {
    Business business = createBusinessRecord.business();

    Cookie authCookie = createBusinessRecord.authCookie();

    testHelper.setCurrentUser(createBusinessRecord.user());

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        createBusinessRecord.allocationRecord().allocation().getId());

    CreateUserRequest userRecord =
        new CreateUserRequest(
            faker.name().firstName(),
            faker.name().lastName(),
            new Address(testHelper.generateEntityAddress()),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber(),
            true);

    String body = objectMapper.writeValueAsString(userRecord);

    MockHttpServletResponse response =
        mvc.perform(post("/users").contentType("application/json").content(body).cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    final TypedId<UserId> createdUserId =
        objectMapper.readValue(response.getContentAsString(), CreateUserResponse.class).getUserId();

    testHelper.setCurrentUser(createBusinessRecord.user());

    userService.retrieveUsersForBusiness(business.getId()).stream()
        .filter(u -> u.getType() == UserType.EMPLOYEE)
        .filter(u -> u.getId().equals(createdUserId))
        .findFirst()
        .ifPresentOrElse((f) -> {}, () -> fail("User was not found"));

    log.info(response.getContentAsString());
  }

  @Test
  @SneakyThrows
  void updateUser_UserHasBeenArchived() {
    final CreateUpdateUserRecord createdUser =
        testHelper.createUser(createBusinessRecord.business());
    createdUser.user().setArchived(true);
    userRepository.saveAndFlush(createdUser.user());
    final UpdateUserRequest userRecord =
        new UpdateUserRequest(
            null,
            null,
            faker.name().firstName(),
            createdUser.user().getLastName().toString(),
            new Address(createdUser.user().getAddress()),
            testHelper.createRandomEmail(), // Ensure non-duplicate email address
            createdUser.user().getPhone().toString(),
            true);

    final String body = objectMapper.writeValueAsString(userRecord);

    final MockHttpServletResponse response =
        mvc.perform(
                patch("/users/" + createdUser.user().getId())
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse();
    final ControllerError error =
        objectMapper.readValue(response.getContentAsString(), ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "User has been archived");
  }

  @SneakyThrows
  @Test
  void updateUser() {
    Business business = createBusinessRecord.business();

    Cookie authCookie = createBusinessRecord.authCookie();

    testHelper.setCurrentUser(createBusinessRecord.user());

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        createBusinessRecord.allocationRecord().allocation().getId());

    CreateUpdateUserRecord createdUser = testHelper.createUser(business);

    UpdateUserRequest userRecord =
        new UpdateUserRequest(
            null,
            null,
            faker.name().firstName(),
            createdUser.user().getLastName().toString(),
            new Address(createdUser.user().getAddress()),
            testHelper.createRandomEmail(), // Ensure non-duplicate email address
            createdUser.user().getPhone().toString(),
            true);

    String body = objectMapper.writeValueAsString(userRecord);

    MockHttpServletResponse response =
        mvc.perform(
                patch("/users/" + createdUser.user().getId())
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    testHelper.setCurrentUser(createBusinessRecord.user());

    com.clearspend.capital.data.model.User user =
        userService.retrieveUser(createdUser.user().getId());
    UpdateUserResponse updatedUser =
        objectMapper.readValue(response.getContentAsString(), UpdateUserResponse.class);
    Assertions.assertEquals(user.getId(), updatedUser.getUserId());
    Assertions.assertEquals(userRecord.getLastName(), user.getLastName().toString());
    Assertions.assertEquals(userRecord.getFirstName(), user.getFirstName().toString());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void testUpdateUserFailsForDuplicateEmailAddresses() {
    Business business = createBusinessRecord.business();

    Cookie authCookie = createBusinessRecord.authCookie();

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        createBusinessRecord.allocationRecord().allocation().getId());

    CreateUpdateUserRecord createdUser = testHelper.createUser(business);

    // Use the Business Owner email to demonstrate that no duplicate emails are permitted
    UpdateUserRequest userRecord =
        new UpdateUserRequest(
            null,
            null,
            faker.name().firstName(),
            createdUser.user().getLastName().toString(),
            new Address(createdUser.user().getAddress()),
            createBusinessRecord.user().getEmail().toString(),
            createdUser.user().getPhone().toString(),
            true);

    String body = objectMapper.writeValueAsString(userRecord);

    MockHttpServletResponse response =
        mvc.perform(
                patch("/users/" + createdUser.user().getId())
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResponse();
  }

  @Test
  @SneakyThrows
  void bulkCreateUser() {
    Business business = createBusinessRecord.business();

    Cookie authCookie = createBusinessRecord.authCookie();

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        createBusinessRecord.allocationRecord().allocation().getId());

    CreateUserRequest userRecord1 =
        new CreateUserRequest(
            faker.name().firstName(),
            faker.name().lastName(),
            new Address(testHelper.generateEntityAddress()),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber(),
            true);
    CreateUserRequest userRecord2 =
        new CreateUserRequest(
            faker.name().firstName(),
            faker.name().lastName(),
            new Address(testHelper.generateEntityAddress()),
            testHelper.createRandomEmail(),
            null,
            true);

    String body = objectMapper.writeValueAsString(List.of(userRecord1, userRecord2));

    MockHttpServletResponse response =
        mvc.perform(
                post("/users/bulk")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    Assertions.assertEquals(
        2, objectMapper.readValue(response.getContentAsString(), List.class).size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getUsers() {
    Business business = createBusinessRecord.business();

    Cookie authCookie = createBusinessRecord.authCookie();

    testHelper.createAllocation(
        business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());

    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "First",
        "Last",
        testHelper.generateEntityAddress(),
        testHelper.createRandomEmail(),
        faker.phoneNumber().phoneNumber());
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        testHelper.createRandomEmail(),
        null); // CAP-519 null phone (which can happen from bulk onboarding) caused NPE

    MockHttpServletResponse response =
        mvc.perform(get("/users/list").contentType("application/json").cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    Assertions.assertTrue(
        objectMapper.readValue(response.getContentAsString(), List.class).size() > 0);
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void customerServiceCanGetUser() {

    CreateBusinessRecord supportOrg = testHelper.createBusiness();
    CreateUpdateUserRecord customerServiceUserCreateRecord =
        testHelper.createUserWithGlobalRole(
            supportOrg.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE);
    Cookie authCookie = testHelper.login(customerServiceUserCreateRecord.user());

    MockHttpServletResponse response =
        mvc.perform(
                get("/users/{userId}", createBusinessRecord.user().getId())
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    com.clearspend.capital.data.model.User user = createBusinessRecord.user();
    User foundUser = objectMapper.readValue(response.getContentAsString(), User.class);
    assertThat(foundUser.getFirstName()).isEqualTo(user.getFirstName().getEncrypted());
    assertThat(foundUser.getLastName()).isEqualTo(user.getLastName().getEncrypted());
    assertThat(foundUser.getAddress().getStreetLine1())
        .isEqualTo(user.getAddress().getStreetLine1().getEncrypted());
    assertThat(foundUser.getAddress().getStreetLine2())
        .isEqualTo(user.getAddress().getStreetLine2().getEncrypted());
    assertThat(foundUser.getAddress().getLocality()).isEqualTo(user.getAddress().getLocality());
    assertThat(foundUser.getAddress().getRegion()).isEqualTo(user.getAddress().getRegion());
    assertThat(foundUser.getEmail()).isEqualTo(user.getEmail().getEncrypted());
    assertThat(foundUser.getPhone()).isEqualTo(user.getPhone().getEncrypted());
  }

  void currentUser() {}

  void getUsersByUserName() {}

  @SneakyThrows
  @Test
  void getUserCards() {
    testHelper.setCurrentUser(createBusinessRecord.user());

    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getBusinessId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final CardAllocation cardAllocation =
        cardAllocationRepository.saveAndFlush(
            new CardAllocation(card.getId(), childAllocation.allocation().getId()));
    final TransactionLimit allocationTransactionLimit =
        transactionLimitRepository
            .findByBusinessIdAndTypeAndOwnerId(
                createBusinessRecord.business().getId(),
                TransactionLimitType.ALLOCATION,
                childAllocation.allocation().getId().toUuid())
            .orElseThrow();
    final TransactionLimit cardAllocationLimit =
        allocationTransactionLimit.copyForType(
            TransactionLimitType.CARD, cardAllocation.getId().toUuid());
    transactionLimitRepository.saveAndFlush(cardAllocationLimit);

    testHelper.setCurrentUser(user.user());

    MockHttpServletResponse response =
        mvc.perform(get("/users/cards").contentType("application/json").cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<CardDetailsResponse> userCardListResponse =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    log.info(
        "\n{}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userCardListResponse));

    for (CardDetailsResponse cardDetailsResponse : userCardListResponse) {
      assertThat(cardDetailsResponse.getCard()).isNotNull();
      assertThat(cardDetailsResponse.getCard().getCardNumber()).isIn(null, card2.getLastFour());

      assertThat(cardDetailsResponse.getAvailableBalance()).isNotNull();
      assertThat(cardDetailsResponse.getAvailableBalance().getCurrency())
          .isEqualTo(business.getCurrency());

      assertThat(cardDetailsResponse.getLedgerBalance()).isNotNull();
      assertThat(cardDetailsResponse.getLedgerBalance().getCurrency())
          .isEqualTo(business.getCurrency());

      if (cardDetailsResponse.getCard().getCardId().equals(card.getId())) {
        assertThat(cardDetailsResponse.getAllowedAllocationsAndLimits()).hasSize(2);
        final List<CardAllocationSpendControls> sorted =
            cardDetailsResponse.getAllowedAllocationsAndLimits().stream()
                .sorted(
                    (d1, d2) ->
                        d1.getAllocationId()
                                .equals(
                                    createBusinessRecord.allocationRecord().allocation().getId())
                            ? -1
                            : 1)
                .toList();
        assertCardAllocationDetailsResponse.doAssert(
            createBusinessRecord.allocationRecord().allocation().getId(), sorted.get(0));
        assertCardAllocationDetailsResponse.doAssert(
            childAllocation.allocation().getId(), sorted.get(1));
      } else {
        assertThat(cardDetailsResponse.getAllowedAllocationsAndLimits()).hasSize(1);
        assertCardAllocationDetailsResponse.doAssert(
            createBusinessRecord.allocationRecord().allocation().getId(),
            cardDetailsResponse.getAllowedAllocationsAndLimits().stream()
                .findFirst()
                .orElseThrow());
      }
    }

    assertThat(userCardListResponse)
        .filteredOn(
            cardDetailsResponse ->
                cardDetailsResponse.getCard().getCardId().equals(card.getId())
                    && !cardDetailsResponse.getCard().isActivated()
                    && cardDetailsResponse.getCard().getActivationDate() == null
                    && cardDetailsResponse.getCard().getStatus() == CardStatus.INACTIVE)
        .hasSize(1);

    assertThat(userCardListResponse)
        .filteredOn(
            cardDetailsResponse ->
                cardDetailsResponse.getCard().getCardId().equals(card2.getId())
                    && cardDetailsResponse.getCard().isActivated()
                    && cardDetailsResponse.getCard().getActivationDate() != null
                    && cardDetailsResponse.getCard().getStatus() == CardStatus.ACTIVE)
        .hasSize(1);
  }

  @SneakyThrows
  @Test
  void getUserCard() {
    MockHttpServletResponse response =
        mvc.perform(
                get("/users/cards/{cardId}", card.getId().toString())
                    .contentType("application/json")
                    .cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CardDetailsResponse cardDetailsResponse =
        objectMapper.readValue(response.getContentAsString(), CardDetailsResponse.class);
    log.info(
        "\n{}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cardDetailsResponse));

    assertThat(cardDetailsResponse.getCard()).isNotNull();
    assertThat(cardDetailsResponse.getCard().getCardNumber()).isNull();
    assertThat(cardDetailsResponse.getCard().isActivated()).isFalse();

    assertThat(cardDetailsResponse.getAvailableBalance()).isNotNull();
    assertThat(cardDetailsResponse.getAvailableBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLedgerBalance()).isNotNull();
    assertThat(cardDetailsResponse.getLedgerBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getAllowedAllocationsAndLimits()).hasSize(1);
    assertCardAllocationDetailsResponse.doAssert(
        createBusinessRecord.allocationRecord().allocation().getId(),
        cardDetailsResponse.getAllowedAllocationsAndLimits().stream().findFirst().orElseThrow());
  }

  @Test
  void blockCard() {
    // given
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    // when
    com.clearspend.capital.controller.type.card.Card blockedCard =
        mockMvcHelper.queryObject(
            "/users/cards/%s/block".formatted(card.getId()),
            HttpMethod.PATCH,
            userCookie,
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);

    // then
    assertThat(blockedCard.getCardId()).isEqualTo(card.getId());
    assertThat(blockedCard.getStatus()).isEqualTo(CardStatus.INACTIVE);
    assertThat(blockedCard.getStatusReason()).isEqualTo(CardStatusReason.CARDHOLDER_REQUESTED);
  }

  @Test
  void unblockCard() {
    // given
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    card.setStatus(CardStatus.INACTIVE);
    cardRepository.saveAndFlush(card);

    // when
    com.clearspend.capital.controller.type.card.Card blockedCard =
        mockMvcHelper.queryObject(
            "/users/cards/%s/unblock".formatted(card.getId()),
            HttpMethod.PATCH,
            userCookie,
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);

    // then
    assertThat(blockedCard.getCardId()).isEqualTo(card.getId());
    assertThat(blockedCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    assertThat(blockedCard.getStatusReason()).isEqualTo(CardStatusReason.CARDHOLDER_REQUESTED);
  }

  @Test
  void cancelCard() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    com.clearspend.capital.controller.type.card.Card cancelledCard =
        mockMvcHelper.queryObject(
            "/users/cards/%s/cancel".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);

    assertThat(cancelledCard)
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);

    final com.stripe.model.issuing.Card stripeCard =
        (com.stripe.model.issuing.Card) stripeMockClient.getCreatedObject(card.getExternalRef());
    assertThat(stripeCard)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", CardUpdateParams.Status.CANCELED.name())
        .hasFieldOrPropertyWithValue("cancellationReason", null);
  }

  @Test
  void cancelCard_Lost() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    com.clearspend.capital.controller.type.card.Card cancelledCard =
        mockMvcHelper.queryObject(
            "/users/cards/%s/cancel".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.LOST),
            com.clearspend.capital.controller.type.card.Card.class);

    assertThat(cancelledCard)
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.LOST);

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.LOST);

    final com.stripe.model.issuing.Card stripeCard =
        (com.stripe.model.issuing.Card) stripeMockClient.getCreatedObject(card.getExternalRef());
    assertThat(stripeCard)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", CardUpdateParams.Status.CANCELED.name())
        .hasFieldOrPropertyWithValue("cancellationReason", CancellationReason.LOST.getValue());
  }

  @Test
  void cancelCard_Stolen() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    com.clearspend.capital.controller.type.card.Card cancelledCard =
        mockMvcHelper.queryObject(
            "/users/cards/%s/cancel".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.STOLEN),
            com.clearspend.capital.controller.type.card.Card.class);

    assertThat(cancelledCard)
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.STOLEN);

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.STOLEN);

    final com.stripe.model.issuing.Card stripeCard =
        (com.stripe.model.issuing.Card) stripeMockClient.getCreatedObject(card.getExternalRef());
    assertThat(stripeCard)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", CardUpdateParams.Status.CANCELED.name())
        .hasFieldOrPropertyWithValue("cancellationReason", CancellationReason.STOLEN.getValue());
  }

  @Test
  @SneakyThrows
  void cancelCard_UserPermissions() {
    final com.clearspend.capital.data.model.User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    final String content =
        objectMapper.writeValueAsString(
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED));

    final ThrowingFunction<Cookie, ResultActions> action =
        cookie -> {
          final Card cardToReset = cardRepository.findById(card.getId()).orElseThrow();
          cardToReset.setStatus(CardStatus.ACTIVE);
          cardRepository.flush();
          return mvc.perform(
              patch("/users/cards/%s/cancel".formatted(card.getId()))
                  .cookie(cookie)
                  .content(content)
                  .contentType("application/json"));
        };
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .denyUser(employee)
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  void activateCard() {
    // given
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // when
    com.clearspend.capital.controller.type.card.Card activeCard =
        mockMvcHelper.queryObject(
            "/users/cards/%s/activate".formatted(card.getId()),
            HttpMethod.PATCH,
            userCookie,
            new ActivateCardRequest(card.getLastFour(), CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);

    // then
    assertThat(activeCard.getCardId()).isEqualTo(card.getId());
    assertThat(activeCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    assertThat(activeCard.getStatusReason()).isEqualTo(CardStatusReason.CARDHOLDER_REQUESTED);
  }

  @Test
  void activateCards() {
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // when
    com.clearspend.capital.controller.type.card.Card receivedCard =
        mockMvcHelper.queryObject(
            "/users/cards/activate",
            HttpMethod.PATCH,
            userCookie,
            new ActivateCardRequest(card.getLastFour(), CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);

    // then
    assertThat(receivedCard.getCardId()).isEqualTo(card.getId());
    assertThat(receivedCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    assertThat(receivedCard.getStatusReason()).isEqualTo(CardStatusReason.CARDHOLDER_REQUESTED);

    // check db for the activated card
    testHelper.setCurrentUser(createBusinessRecord.user());
    assertThat(cardService.getCard(business.getId(), receivedCard.getCardId()).card().isActivated())
        .isTrue();
  }

  @SneakyThrows
  @Test
  void getCardAccountActivity() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(100L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord userRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_MANAGER);

    Cookie authCookie = testHelper.login(userRecord.user());
    testHelper.setCurrentUser(createBusinessRecord.user());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of());
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(), userRecord.user().getId(), Currency.USD, true, List.of(controls));
    issueCardRequest.setFundingType(FundingType.POOLED);
    issueCardRequest.setShippingAddress(
        new com.clearspend.capital.controller.type.Address(
            createBusinessRecord.business().getClearAddress()));

    CardRecord cardRecord =
        cardService.issueCard(
            createBusinessRecord.business().getId(), CardType.VIRTUAL, issueCardRequest);

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    MockHttpServletResponse response =
        mvc.perform(
                get("/users/cards/{cardId}/account-activity", cardRecord.card().getId())
                    .param("dateFrom", "2020-01-01T00:00:00.000Z")
                    .param("dateTo", "2030-01-01T00:00:00.000Z")
                    .param("pageNumber", "0")
                    .param("pageSize", "20")
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @Test
  @SneakyThrows
  void unlinkCard_PooledCard() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    final String response =
        mvc.perform(
                patch("/users/cards/%s/unlink".formatted(card.getId()))
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final CardAndAccount cardAndAccount = objectMapper.readValue(response, CardAndAccount.class);

    card.setAllocationId(null);
    card.setAccountId(null);
    assertThat(cardAndAccount)
        .hasFieldOrPropertyWithValue(
            "card", new com.clearspend.capital.controller.type.card.Card(card))
        .hasFieldOrPropertyWithValue("account", null);

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard).isEqualTo(card);
  }

  @Test
  @SneakyThrows
  void unlinkCard_IndividualCard() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.INDIVIDUAL,
            CardType.PHYSICAL,
            false);
    final Account cardAccount = accountRepository.findById(card.getAccountId()).orElseThrow();
    final String response =
        mvc.perform(
                patch("/users/cards/%s/unlink".formatted(card.getId()))
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final CardAndAccount cardAndAccount = objectMapper.readValue(response, CardAndAccount.class);

    card.setAllocationId(null);
    cardAccount.setAllocationId(null);
    assertThat(cardAndAccount)
        .hasFieldOrPropertyWithValue(
            "card", new com.clearspend.capital.controller.type.card.Card(card))
        .hasFieldOrPropertyWithValue(
            "account", com.clearspend.capital.controller.type.account.Account.of(cardAccount));
  }

  @Test
  @SneakyThrows
  void unlinkCard_IsAlreadyUnlinked() {
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    card.setAllocationId(null);
    card.setAccountId(null);
    card = cardRepository.saveAndFlush(card);

    mvc.perform(
            patch("/users/cards/%s/unlink".formatted(card.getId()))
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unlinkCard_UserPermissions() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie -> {
          final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
          dbCard.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
          dbCard.setAccountId(createBusinessRecord.allocationRecord().account().getId());
          cardRepository.saveAndFlush(dbCard);

          return mvc.perform(
              patch("/users/cards/%s/unlink".formatted(card.getId())).cookie(cookie));
        };

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
  void getCardAccountActivity_usesCriteriaCardIdIfAvailable() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(100L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord userRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_MANAGER);

    Cookie authCookie = testHelper.login(userRecord.user());
    testHelper.setCurrentUser(createBusinessRecord.user());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of());
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(), userRecord.user().getId(), Currency.USD, true, List.of(controls));
    issueCardRequest.setFundingType(FundingType.POOLED);
    issueCardRequest.setShippingAddress(
        new com.clearspend.capital.controller.type.Address(
            createBusinessRecord.business().getClearAddress()));

    CardRecord cardRecord =
        cardService.issueCard(
            createBusinessRecord.business().getId(), CardType.VIRTUAL, issueCardRequest);

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    AccountActivityRequest criteria = new AccountActivityRequest();
    criteria.setCardId(cardRecord.card().getId());
    criteria.setFrom(
        OffsetDateTime.of(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0), ZoneOffset.UTC));
    criteria.setTo(
        OffsetDateTime.of(LocalDateTime.of(2030, Month.JANUARY, 1, 0, 0), ZoneOffset.UTC));
    criteria.setPageRequest(new PageRequest(0, 20));

    MockHttpServletResponse response =
        mvc.perform(
                post("/users/cards/{cardId}/account-activity", UUID.randomUUID().toString())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(criteria))
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getAccountActivity() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(100L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord userRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_MANAGER);

    Cookie authCookie = testHelper.login(userRecord.user());
    testHelper.setCurrentUser(createBusinessRecord.user());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of());
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(), userRecord.user().getId(), Currency.USD, true, List.of(controls));
    issueCardRequest.setFundingType(FundingType.POOLED);
    issueCardRequest.setShippingAddress(
        new com.clearspend.capital.controller.type.Address(
            createBusinessRecord.business().getClearAddress()));

    CardRecord cardRecord =
        cardService.issueCard(
            createBusinessRecord.business().getId(), CardType.VIRTUAL, issueCardRequest);

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });

    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
    TypedId<AccountActivityId> id =
        networkCommonAuthorization.networkCommon().getAccountActivity().getId();

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format("/users/account-activity/%s", id))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertEquals(
        id,
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getAccountActivityId());
  }

  @SneakyThrows
  @Test
  void updateAccountActivity() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(100L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord userRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_MANAGER);

    Cookie authCookie = testHelper.login(userRecord.user());
    testHelper.setCurrentUser(createBusinessRecord.user());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of());
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(), userRecord.user().getId(), Currency.USD, true, List.of(controls));
    issueCardRequest.setFundingType(FundingType.POOLED);
    issueCardRequest.setShippingAddress(
        new com.clearspend.capital.controller.type.Address(
            createBusinessRecord.business().getClearAddress()));

    CardRecord cardRecord =
        cardService.issueCard(
            createBusinessRecord.business().getId(), CardType.VIRTUAL, issueCardRequest);

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
    TypedId<AccountActivityId> id =
        networkCommonAuthorization.networkCommon().getAccountActivity().getId();

    ExpenseCategory expenseCategory =
        new ExpenseCategory(
            createBusinessRecord.business().getId(),
            0,
            "TestCategory",
            ExpenseCategoryStatus.ACTIVE,
            false);

    expenseCategoryRepository.save(expenseCategory);

    UpdateAccountActivityRequest updateAccountActivityRequest =
        new UpdateAccountActivityRequest(
            "notesTest", expenseCategory.getId(), "0", "Test Supplier");

    MockHttpServletResponse response =
        mvc.perform(
                patch(String.format("/users/account-activity/%s", id))
                    .content(objectMapper.writeValueAsString(updateAccountActivityRequest))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertEquals(
        "notesTest",
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getNotes());
    Assertions.assertEquals(
        expenseCategory.getId(),
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getExpenseDetails()
            .getExpenseCategoryId());
  }

  @SneakyThrows
  @Test
  void updateAccountActivity_IconRefOptionalEmptyCase() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(100L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord userRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_MANAGER);

    Cookie authCookie = testHelper.login(userRecord.user());
    testHelper.setCurrentUser(createBusinessRecord.user());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of());
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(), userRecord.user().getId(), Currency.USD, true, List.of(controls));
    issueCardRequest.setFundingType(FundingType.POOLED);
    issueCardRequest.setShippingAddress(
        new com.clearspend.capital.controller.type.Address(
            createBusinessRecord.business().getClearAddress()));

    CardRecord cardRecord =
        cardService.issueCard(
            createBusinessRecord.business().getId(), CardType.VIRTUAL, issueCardRequest);

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
    TypedId<AccountActivityId> id =
        networkCommonAuthorization.networkCommon().getAccountActivity().getId();

    ExpenseCategory expenseCategory =
        new ExpenseCategory(
            createBusinessRecord.business().getId(),
            0,
            "TestCategory",
            ExpenseCategoryStatus.ACTIVE,
            false);

    expenseCategoryRepository.save(expenseCategory);

    UpdateAccountActivityRequest updateAccountActivityRequest =
        new UpdateAccountActivityRequest(
            "notesTest", expenseCategory.getId(), "0", "Test Supplier");

    MockHttpServletResponse response =
        mvc.perform(
                patch(String.format("/users/account-activity/%s", id))
                    .content(objectMapper.writeValueAsString(updateAccountActivityRequest))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertEquals(
        "notesTest",
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getNotes());
    Assertions.assertEquals(
        expenseCategory.getId(),
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getExpenseDetails()
            .getExpenseCategoryId());

    // For case when iconRef will be Optional.empty the values should be deleted
    updateAccountActivityRequest =
        new UpdateAccountActivityRequest("notesTest", null, "0", "Test Supplier");

    response =
        mvc.perform(
                patch(String.format("/users/account-activity/%s", id))
                    .content(objectMapper.writeValueAsString(updateAccountActivityRequest))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertEquals(
        "notesTest",
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getNotes());
    Assertions.assertNull(
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getExpenseDetails());
  }

  @SneakyThrows
  @Test
  void updateAccountActivity_IconRefNullCase() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(100L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord userRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_MANAGER);

    Cookie authCookie = testHelper.login(userRecord.user());
    testHelper.setCurrentUser(createBusinessRecord.user());

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of());
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(), userRecord.user().getId(), Currency.USD, true, List.of(controls));
    issueCardRequest.setFundingType(FundingType.POOLED);
    issueCardRequest.setShippingAddress(
        new com.clearspend.capital.controller.type.Address(
            createBusinessRecord.business().getClearAddress()));

    CardRecord cardRecord =
        cardService.issueCard(
            createBusinessRecord.business().getId(), CardType.VIRTUAL, issueCardRequest);

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
    TypedId<AccountActivityId> id =
        networkCommonAuthorization.networkCommon().getAccountActivity().getId();

    ExpenseCategory expenseCategory =
        new ExpenseCategory(
            createBusinessRecord.business().getId(),
            0,
            "TestCategory",
            ExpenseCategoryStatus.ACTIVE,
            false);

    expenseCategoryRepository.save(expenseCategory);

    UpdateAccountActivityRequest updateAccountActivityRequest =
        new UpdateAccountActivityRequest(
            "notesTest", expenseCategory.getId(), "0", "Test Supplier");

    MockHttpServletResponse response =
        mvc.perform(
                patch(String.format("/users/account-activity/%s", id))
                    .content(objectMapper.writeValueAsString(updateAccountActivityRequest))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertEquals(
        "notesTest",
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getNotes());
    Assertions.assertEquals(
        expenseCategory.getId(),
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getExpenseDetails()
            .getExpenseCategoryId());

    // For case when iconRef will be null the expense category values should be deleted
    response =
        mvc.perform(
                patch(String.format("/users/account-activity/%s", id))
                    .content("{\"notes\":\"notesTest\"}")
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertEquals(
        "notesTest",
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getNotes());
    Assertions.assertNull(
        objectMapper
            .readValue(response.getContentAsString(), AccountActivityResponse.class)
            .getExpenseDetails());
  }

  @Test
  @SneakyThrows
  void linkReceipt() {
    final AccountActivity accountActivity =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(createBusinessRecord.user())
                .build());
    final Receipt receipt =
        testDataHelper.createReceipt(
            ReceiptConfig.fromCreateBusinessRecord(createBusinessRecord).build());

    mvc.perform(
            post("/users/account-activity/%s/receipts/%s/link"
                    .formatted(
                        accountActivity.getId().toUuid().toString(),
                        receipt.getId().toUuid().toString()))
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    final AccountActivity resultActivity =
        accountActivityRepo.findById(accountActivity.getId()).orElseThrow();
    final Receipt resultReceipt = receiptRepo.findById(receipt.getId()).orElseThrow();

    assertThat(resultReceipt)
        .hasFieldOrPropertyWithValue("linkUserIds", Set.of(accountActivity.getUserDetailsId()));
    assertThat(resultActivity.getReceipt())
        .hasFieldOrPropertyWithValue("receiptIds", Set.of(receipt.getId()));
  }

  @Test
  @SneakyThrows
  void unlinkReceipt() {
    AccountActivity accountActivity =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(createBusinessRecord.user())
                .build());
    Receipt receipt =
        testDataHelper.createReceipt(
            ReceiptConfig.fromCreateBusinessRecord(createBusinessRecord).build());

    accountActivity.getReceipt().getReceiptIds().add(receipt.getId());
    accountActivity = accountActivityRepo.save(accountActivity);

    receipt.addLinkUserId(createBusinessRecord.user().getId());
    receipt.setLinked(true);
    receipt = receiptRepo.save(receipt);

    mvc.perform(
            post("/users/account-activity/%s/receipts/%s/unlink"
                    .formatted(
                        accountActivity.getId().toUuid().toString(),
                        receipt.getId().toUuid().toString()))
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    final AccountActivity resultActivity =
        accountActivityRepo.findById(accountActivity.getId()).orElseThrow();
    final Receipt resultReceipt = receiptRepo.findById(receipt.getId()).orElseThrow();

    assertThat(resultReceipt).hasFieldOrPropertyWithValue("linkUserIds", Set.of());
    assertThat(resultActivity.getReceipt()).hasFieldOrPropertyWithValue("receiptIds", Set.of());
  }

  @Test
  @SneakyThrows
  void deleteReceipt() {
    AccountActivity accountActivity =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord)
                .owner(createBusinessRecord.user())
                .build());
    Receipt receipt =
        testDataHelper.createReceipt(
            ReceiptConfig.fromCreateBusinessRecord(createBusinessRecord).build());

    accountActivity.getReceipt().getReceiptIds().add(receipt.getId());
    accountActivity = accountActivityRepo.save(accountActivity);

    receipt.addLinkUserId(createBusinessRecord.user().getId());
    receipt.setLinked(true);
    receipt = receiptRepo.save(receipt);

    mvc.perform(
            delete("/users/receipts/%s/delete".formatted(receipt.getId().toUuid().toString()))
                .cookie(createBusinessRecord.authCookie()))
        .andExpect(status().isOk());

    final AccountActivity resultActivity =
        accountActivityRepo.findById(accountActivity.getId()).orElseThrow();
    assertThat(receiptRepo.findById(receipt.getId())).isEmpty();

    assertThat(resultActivity.getReceipt()).hasFieldOrPropertyWithValue("receiptIds", Set.of());
  }

  @SneakyThrows
  @Test
  void getUsersForBusinessIdByUserName() {
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createAllocation(
        business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());

    CreateUpdateUserRecord userRecord =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        testHelper.createRandomEmail(),
        faker.phoneNumber().phoneNumber());

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                get("/users/list")
                    .queryParam(
                        Common.USER_NAME,
                        userRecord.user().getFirstName().toString().substring(0, 3))
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    // FIXME(hdavid) this assertion is invalid, it does not check the returned usernames
    Assertions.assertTrue(
        objectMapper.readValue(responseFilteredByUserName.getContentAsString(), List.class).size()
            > 0);
    log.info(responseFilteredByUserName.getContentAsString());
  }

  @Test
  void getUsersForBusinessIdOfNonLoggedInUser() {
    CreateBusinessRecord foreignBusiness = testHelper.createBusiness();
    testHelper.setCurrentUser(foreignBusiness.user());

    final String userFromForeignBusinessName = "userFromForeignBusiness";
    CreateUpdateUserRecord userFromForeignBusiness =
        userService.createUser(
            foreignBusiness.business().getBusinessId(),
            UserType.EMPLOYEE,
            userFromForeignBusinessName,
            "LastName",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    Business business = createBusinessRecord.business();
    CreateUpdateUserRecord userWithCustomerServiceRole =
        testHelper.createUserWithGlobalRole(business, DefaultRoles.GLOBAL_CUSTOMER_SERVICE);
    Cookie authCookie = testHelper.login(userWithCustomerServiceRole.user());
    testHelper.setCurrentUser(userWithCustomerServiceRole.user());

    final ThrowingFunction<Cookie, ResultActions> requestUsersFromLogggedInBusinessUser =
        cookie ->
            mvc.perform(get("/users/list").contentType("application/json").cookie(authCookie));

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowAllRolesOnAllocation()
        .<ResultActions>allowAllGlobalRolesWithResult(
            ra -> {
              Assertions.assertFalse(
                  objectMapper
                      .readValue(
                          ra.andReturn().getResponse().getContentAsString(),
                          new TypeReference<List<User>>() {})
                      .stream()
                      .anyMatch(u -> u.getFirstName().equals(userFromForeignBusinessName)));
            })
        .build()
        .validateMockMvcCall(requestUsersFromLogggedInBusinessUser);

    final ThrowingFunction<Cookie, ResultActions> requestUsersFromForeignBusiness =
        cookie ->
            mvc.perform(
                get("/users/list")
                    .header(Common.BUSINESS_ID, foreignBusiness.business().getBusinessId())
                    .contentType("application/json")
                    .cookie(authCookie));

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowAllGlobalRoles()
        .<ResultActions>allowAllRolesOnAllocationWithResult(
            ra -> {
              Assertions.assertTrue(
                  objectMapper
                      .readValue(
                          ra.andReturn().getResponse().getContentAsString(),
                          new TypeReference<List<User>>() {})
                      .stream()
                      .anyMatch(u -> u.getFirstName().equals(userFromForeignBusinessName)));
            })
        .build()
        .validateMockMvcCall(requestUsersFromForeignBusiness);
  }

  @SneakyThrows
  @Test
  void searchForUsers() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    AllocationService.AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    testHelper.issueCard(
        business,
        allocation.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    testHelper.issueCard(
        business,
        allocation.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    testHelper.issueCard(
        business,
        allocation.allocation(),
        user1.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setWithoutCard(false);
    searchUserRequest.setHasPhysicalCard(false);
    searchUserRequest.setHasVirtualCard(false);
    searchUserRequest.setAllocations(List.of());
    searchUserRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(searchUserRequest);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                post("/users/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(responseFilteredByUserName.getContentAsString());
    PagedData<UserPageData> userPageData =
        objectMapper.readValue(
            responseFilteredByUserName.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, UserPageData.class));
    Assertions.assertEquals(3, userPageData.getTotalElements());
  }

  @SneakyThrows
  @Test
  void searchForUsersFilteredByAllocation() {
    Business business = createBusinessRecord.business();
    AllocationService.AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "allocation1",
            createBusinessRecord.allocationRecord().allocation().getId());

    AllocationService.AllocationRecord allocation2 =
        testHelper.createAllocation(
            business.getId(),
            "allocation2",
            createBusinessRecord.allocationRecord().allocation().getId());

    AllocationService.AllocationRecord allocation3 =
        testHelper.createAllocation(
            business.getId(),
            "allocation3",
            createBusinessRecord.allocationRecord().allocation().getId());

    Cookie authCookie = createBusinessRecord.authCookie();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    testHelper.issueCard(
        business,
        allocation.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    testHelper.issueCard(
        business,
        allocation2.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    testHelper.issueCard(
        business,
        allocation3.allocation(),
        user1.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setAllocations(List.of(allocation3.allocation().getId()));
    searchUserRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(searchUserRequest);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                post("/users/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(responseFilteredByUserName.getContentAsString());
    PagedData<UserPageData> userPageData =
        objectMapper.readValue(
            responseFilteredByUserName.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, UserPageData.class));
    Assertions.assertEquals(1, userPageData.getTotalElements());
  }

  @SneakyThrows
  @Test
  void searchForUsersFilteredByCardType() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());

    AllocationService.AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "allocation1",
            createBusinessRecord.allocationRecord().allocation().getId());

    AllocationService.AllocationRecord allocation2 =
        testHelper.createAllocation(
            business.getId(),
            "allocation2",
            createBusinessRecord.allocationRecord().allocation().getId());

    AllocationService.AllocationRecord allocation3 =
        testHelper.createAllocation(
            business.getId(),
            "allocation3",
            createBusinessRecord.allocationRecord().allocation().getId());

    Cookie authCookie = createBusinessRecord.authCookie();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.issueCard(
        business,
        allocation.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    testHelper.issueCard(
        business,
        allocation2.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    testHelper.issueCard(
        business,
        allocation3.allocation(),
        user1.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setHasPhysicalCard(true);
    searchUserRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(searchUserRequest);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                post("/users/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(responseFilteredByUserName.getContentAsString());
    PagedData<UserPageData> userPageData =
        objectMapper.readValue(
            responseFilteredByUserName.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, UserPageData.class));
    log.debug("userPageData: {}", userPageData);
    Assertions.assertEquals(2, userPageData.getTotalElements());
    testHelper.setCurrentUser(createBusinessRecord.user());
    Assertions.assertTrue(
        userPageData.getContent().get(0).getCardInfoList().stream()
            .anyMatch(
                cardInfo ->
                    cardService
                        .retrieveCard(business.getId(), cardInfo.getCardId())
                        .getType()
                        .equals(CardType.PHYSICAL)));
  }

  @SneakyThrows
  @Test
  void searchForUsersFilteredBySmartSearch() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "First",
        "Last",
        testHelper.generateEntityAddress(),
        testHelper.createRandomEmail(),
        faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setSearchText("Name");
    searchUserRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(searchUserRequest);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                post("/users/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(responseFilteredByUserName.getContentAsString());
    PagedData<UserPageData> userPageData =
        objectMapper.readValue(
            responseFilteredByUserName.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, UserPageData.class));
    Assertions.assertEquals(1, userPageData.getTotalElements());
    Assertions.assertEquals(
        user1.user().getFirstName().getEncrypted(),
        userPageData.getContent().get(0).getUserData().getFirstName());
  }

  @Test
  void archiveUser() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final com.clearspend.capital.data.model.User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    // Confirming the user is in FusionAuth prior to test logic
    assertDoesNotThrow(() -> serviceHelper.coreFusionAuthService().getUser(employee));

    final TypedId<CardId> virtualCardId =
        testHelper
            .issueCard(
                createBusinessRecord.business(),
                createBusinessRecord.allocationRecord().allocation(),
                employee,
                Currency.USD,
                FundingType.POOLED,
                CardType.VIRTUAL,
                false)
            .getId();
    final TypedId<CardId> physicalCardId =
        testHelper
            .issueCard(
                createBusinessRecord.business(),
                createBusinessRecord.allocationRecord().allocation(),
                employee,
                Currency.USD,
                FundingType.POOLED,
                CardType.PHYSICAL,
                true)
            .getId();

    final boolean result =
        mockMvcHelper.queryObject(
            "/users/%s/archive".formatted(employee.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            Boolean.class);
    assertTrue(result);

    final com.clearspend.capital.data.model.User dbEmployee =
        userRepository.findById(employee.getId()).orElseThrow();
    assertThat(dbEmployee).hasFieldOrPropertyWithValue("archived", true);

    final Card dbVirtualCard = cardRepository.findById(virtualCardId).orElseThrow();
    assertThat(dbVirtualCard)
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.USER_ARCHIVED);

    final Card dbPhysicalCard = cardRepository.findById(physicalCardId).orElseThrow();
    assertThat(dbPhysicalCard)
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.USER_ARCHIVED);

    final com.stripe.model.issuing.Card stripeVirtualCard =
        (com.stripe.model.issuing.Card)
            stripeMockClient.getCreatedObject(dbVirtualCard.getExternalRef());
    assertThat(stripeVirtualCard)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", CardUpdateParams.Status.CANCELED.name());

    final com.stripe.model.issuing.Card stripePhysicalCard =
        (com.stripe.model.issuing.Card)
            stripeMockClient.getCreatedObject(dbPhysicalCard.getExternalRef());
    assertThat(stripePhysicalCard)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", CardUpdateParams.Status.CANCELED.name());

    assertThrows(
        FusionAuthException.class, () -> serviceHelper.coreFusionAuthService().getUser(employee));
  }

  @Test
  void archiveUser_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final ThrowingFunction<Cookie, ResultActions> action =
        cookie -> {
          final com.clearspend.capital.data.model.User employee =
              testHelper.runWithCurrentUser(
                  createBusinessRecord.user(),
                  () ->
                      testHelper
                          .createUserWithRole(
                              createBusinessRecord.allocationRecord().allocation(),
                              DefaultRoles.ALLOCATION_EMPLOYEE)
                          .user());
          return mockMvcHelper.query(
              "/users/%s/archive".formatted(employee.getId()), HttpMethod.PATCH, cookie);
        };
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateMockMvcCall(action);
  }

  @SneakyThrows
  @Test
  void searchForUsersExcludeArchivedUsers() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    userService.archiveUser(business.getId(), user.user().getId());
    userService.archiveUser(business.getId(), user1.user().getId());

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(searchUserRequest);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                post("/users/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(responseFilteredByUserName.getContentAsString());
    PagedData<UserPageData> userPageData =
        objectMapper.readValue(
            responseFilteredByUserName.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, UserPageData.class));
    Assertions.assertEquals(1, userPageData.getTotalElements());
  }

  @Test
  @SneakyThrows
  void archiveUser_UserAlreadyArchived() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final com.clearspend.capital.data.model.User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    employee.setArchived(true);
    userRepository.saveAndFlush(employee);

    final String response =
        mockMvcHelper
            .query(
                "/users/%s/archive".formatted(employee.getId()),
                HttpMethod.PATCH,
                createBusinessRecord.authCookie())
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final ControllerError error = objectMapper.readValue(response, ControllerError.class);
    assertThat(error).hasFieldOrPropertyWithValue("message", "User is already archived");
  }

  @SneakyThrows
  @Test
  void searchForUsersIncludeArchivedUsers() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    testHelper.setCurrentUser(createBusinessRecord.user());

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    userService.archiveUser(business.getId(), user.user().getId());
    userService.archiveUser(business.getId(), user1.user().getId());

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setIncludeArchived(true);
    searchUserRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(searchUserRequest);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                post("/users/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(responseFilteredByUserName.getContentAsString());
    PagedData<UserPageData> userPageData =
        objectMapper.readValue(
            responseFilteredByUserName.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, UserPageData.class));
    Assertions.assertEquals(3, userPageData.getTotalElements());
  }

  @SneakyThrows
  @Test
  void searchForUsersWithoutCard() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());

    AllocationService.AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "allocation1",
            createBusinessRecord.allocationRecord().allocation().getId());

    AllocationService.AllocationRecord allocation2 =
        testHelper.createAllocation(
            business.getId(),
            "allocation2",
            createBusinessRecord.allocationRecord().allocation().getId());

    Cookie authCookie = createBusinessRecord.authCookie();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            testHelper.createRandomEmail(),
            faker.phoneNumber().phoneNumber());

    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.issueCard(
        business,
        allocation.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    testHelper.issueCard(
        business,
        allocation2.allocation(),
        user.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setWithoutCard(true);
    searchUserRequest.setPageRequest(new PageRequest(0, 10));

    String body = objectMapper.writeValueAsString(searchUserRequest);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                post("/users/search")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(responseFilteredByUserName.getContentAsString());
    PagedData<UserPageData> userPageData =
        objectMapper.readValue(
            responseFilteredByUserName.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, UserPageData.class));
    log.debug("userPageData: {}", userPageData);
    Assertions.assertEquals(2, userPageData.getTotalElements());
    Assertions.assertEquals(
        user1.user().getFirstName().getEncrypted(),
        userPageData.getContent().stream()
            .filter(userData -> userData.getUserData().getType() == UserType.EMPLOYEE)
            .findFirst()
            .orElseThrow()
            .getUserData()
            .getFirstName());
    Assertions.assertEquals(
        createBusinessRecord.businessOwner().getFirstName().getEncrypted(),
        userPageData.getContent().stream()
            .filter(userData -> userData.getUserData().getType() == UserType.BUSINESS_OWNER)
            .findFirst()
            .orElseThrow()
            .getUserData()
            .getFirstName());
  }

  @Test
  void getCardAccounts_AllocationAccount_CardIsUnlinked() {
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
    final JavaType responseType =
        objectMapper.getTypeFactory().constructParametricType(List.class, CardAccount.class);
    final List<CardAccount> response =
        mockMvcHelper.queryObject(
            "/users/cards/%s/accounts?type=ALLOCATION".formatted(card.getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            responseType);
    final Account account = createBusinessRecord.allocationRecord().account();
    assertThat(response)
        .hasSize(1)
        .contains(
            new CardAccount(
                account.getAllocationId(),
                account.getId(),
                account.getType(),
                com.clearspend.capital.controller.type.Amount.of(account.getLedgerBalance())));
  }

  @Test
  void getCardAccounts_CardAccount_CardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.INDIVIDUAL,
            CardType.PHYSICAL,
            true);
    final Account account = accountRepository.findById(card.getAccountId()).orElseThrow();
    account.setAllocationId(null);
    card.setAllocationId(null);
    cardRepository.saveAndFlush(card);
    accountRepository.saveAndFlush(account);

    final JavaType responseType =
        objectMapper.getTypeFactory().constructParametricType(List.class, CardAccount.class);
    final List<CardAccount> response =
        mockMvcHelper.queryObject(
            "/users/cards/%s/accounts?type=CARD".formatted(card.getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            responseType);
    assertThat(response)
        .hasSize(1)
        .contains(
            new CardAccount(
                account.getAllocationId(),
                account.getId(),
                account.getType(),
                com.clearspend.capital.controller.type.Amount.of(account.getLedgerBalance())));
  }

  @Test
  @SneakyThrows
  void blockCard_CardIsUnlinked() {
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
    com.clearspend.capital.controller.type.card.Card result =
        mockMvcHelper.queryObject(
            "/users/cards/%s/block".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);
    assertThat(result)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("status", CardStatus.INACTIVE)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("status", CardStatus.INACTIVE)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);
  }

  @Test
  void unblockCard_CardIsUnlinked() {
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
    card.setStatus(CardStatus.INACTIVE);
    cardRepository.saveAndFlush(card);
    com.clearspend.capital.controller.type.card.Card result =
        mockMvcHelper.queryObject(
            "/users/cards/%s/unblock".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);
    assertThat(result)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("status", CardStatus.ACTIVE)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("status", CardStatus.ACTIVE)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);
  }

  @Test
  void cancelCard_CardIsUnlinked() {
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
    com.clearspend.capital.controller.type.card.Card result =
        mockMvcHelper.queryObject(
            "/users/cards/%s/cancel".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);
    assertThat(result)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("status", CardStatus.CANCELLED)
        .hasFieldOrPropertyWithValue("statusReason", CardStatusReason.CARDHOLDER_REQUESTED);
  }

  @Test
  void getUserCards_OneCardIsUnlinked() {
    final Card linkedCard =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    final Card unlinkedCArd =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    unlinkedCArd.setAllocationId(null);
    unlinkedCArd.setAccountId(null);
    cardRepository.saveAndFlush(unlinkedCArd);

    final JavaType responseType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(List.class, CardDetailsResponse.class);
    final List<CardDetailsResponse> response =
        mockMvcHelper.queryObject(
            "/users/cards", HttpMethod.GET, createBusinessRecord.authCookie(), responseType);
    assertThat(response).hasSize(2);
    final List<CardDetailsResponse> unlinkedCards =
        response.stream().filter(item -> item.getCard().getAllocationId() == null).toList();
    assertThat(unlinkedCards).hasSize(1);
    assertThat(unlinkedCards.get(0).getCard())
        .hasFieldOrPropertyWithValue("cardId", unlinkedCArd.getId())
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null);
  }

  @Test
  void getUserCard_CardIsUnlinked() {
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
            "/users/cards/%s".formatted(card.getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
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
  void activateMyCard_CardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);
    assertThat(card).hasFieldOrPropertyWithValue("status", CardStatus.INACTIVE);

    final ActivateCardRequest request =
        new ActivateCardRequest(card.getLastFour(), CardStatusReason.CARDHOLDER_REQUESTED);
    final com.clearspend.capital.controller.type.card.Card response =
        mockMvcHelper.queryObject(
            "/users/cards/%s/activate".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            request,
            com.clearspend.capital.controller.type.card.Card.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("status", CardStatus.ACTIVE)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null);
  }

  @Test
  @SneakyThrows
  void activateMyCards_UserHasBeenArchived() {
    final CreateUpdateUserRecord employeeRecord =
        testHelper.createUserWithRole(
            createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_EMPLOYEE);
    employeeRecord.user().setArchived(true);
    userRepository.saveAndFlush(employeeRecord.user());
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    final ActivateCardRequest request =
        new ActivateCardRequest(card.getLastFour(), CardStatusReason.CARDHOLDER_REQUESTED);
    final Cookie cookie = testHelper.login(employeeRecord.user());

    // An archived user has no permissions, so PreFilter removes all cards from request, which
    // results in RecordNotFound, ie 404
    mockMvcHelper
        .query("/users/cards/activate", HttpMethod.PATCH, cookie, request)
        .andExpect(status().is(404));
  }

  @Test
  void activateMyCards_CardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);
    assertThat(card).hasFieldOrPropertyWithValue("status", CardStatus.INACTIVE);

    final ActivateCardRequest request =
        new ActivateCardRequest(card.getLastFour(), CardStatusReason.CARDHOLDER_REQUESTED);
    final com.clearspend.capital.controller.type.card.Card response =
        mockMvcHelper.queryObject(
            "/users/cards/activate",
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            request,
            com.clearspend.capital.controller.type.card.Card.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("status", CardStatus.ACTIVE)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null);
  }

  @Test
  void linkCard() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    cardService.addAllocationsToCard(
        card, List.of(new CardAllocationSpendControls(childAllocation.allocation().getId())));

    final CardAndAccount response =
        mockMvcHelper.queryObject(
            "/users/cards/%s/link/%s".formatted(card.getId(), childAllocation.allocation().getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            CardAndAccount.class);
    assertThat(response.card())
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("allocationId", childAllocation.allocation().getId());
    assertThat(response.account())
        .hasFieldOrPropertyWithValue("accountId", childAllocation.account().getId());

    final Card dbCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(dbCard)
        .hasFieldOrPropertyWithValue("allocationId", childAllocation.allocation().getId())
        .hasFieldOrPropertyWithValue("accountId", childAllocation.account().getId());
  }

  @Test
  void linkCard_UserPermissions() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final com.clearspend.capital.data.model.User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    testHelper.setCurrentUser(createBusinessRecord.user());
    cardService.addAllocationsToCard(
        card, List.of(new CardAllocationSpendControls(childAllocation.allocation().getId())));

    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvcHelper.query(
                "/users/cards/%s/link/%s"
                    .formatted(card.getId(), childAllocation.allocation().getId()),
                HttpMethod.PATCH,
                cookie);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(employee)
        .build()
        .validateMockMvcCall(action);
  }

  @Test
  @SneakyThrows
  void linkCard_AllocationNotAllowed() {
    final AllocationRecord childAllocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            createBusinessRecord.allocationRecord().allocation().getId());
    final Card card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    mockMvcHelper
        .query(
            "/users/cards/%s/link/%s".formatted(card.getId(), childAllocation.allocation().getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie())
        .andExpect(status().isBadRequest());
  }
}
