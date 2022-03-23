package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.activity.UpdateAccountActivityRequest;
import com.clearspend.capital.controller.type.card.ActivateCardRequest;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
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
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class UserControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mockMvcHelper;
  private final TestHelper testHelper;
  private final CardService cardService;
  private final NetworkMessageService networkMessageService;
  private final UserService userService;
  private final CardRepository cardRepository;
  private final ExpenseCategoryRepository expenseCategoryRepository;

  private final Faker faker = new Faker();

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private CreateUpdateUserRecord user;
  private Cookie userCookie;
  private Card card;
  private Card card2;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.init();
      business = createBusinessRecord.business();
      user =
          testHelper.createUserWithRole(
              createBusinessRecord.allocationRecord().allocation(),
              DefaultRoles.ALLOCATION_EMPLOYEE);
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
    }
    testHelper.setCurrentUser(createBusinessRecord.user());
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

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        createBusinessRecord.allocationRecord().allocation().getId(),
        createBusinessRecord.user());

    CreateUserRequest userRecord =
        new CreateUserRequest(
            faker.name().firstName(),
            faker.name().lastName(),
            new Address(testHelper.generateEntityAddress()),
            faker.internet().emailAddress(),
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

    userService.retrieveUsersForBusiness(business.getId()).stream()
        .filter(u -> u.getType() == UserType.EMPLOYEE)
        .filter(u -> u.getId().equals(createdUserId))
        .findFirst()
        .ifPresentOrElse((f) -> {}, () -> fail("User was not found"));

    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void updateUser() {
    Business business = createBusinessRecord.business();

    Cookie authCookie = createBusinessRecord.authCookie();

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        createBusinessRecord.allocationRecord().allocation().getId(),
        createBusinessRecord.user());

    CreateUpdateUserRecord createdUser = testHelper.createUser(business);

    UpdateUserRequest userRecord =
        new UpdateUserRequest(
            faker.name().firstName(),
            createdUser.user().getLastName().toString(),
            new Address(createdUser.user().getAddress()),
            faker.internet().emailAddress(), // Ensure non-duplicate email address
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
        createBusinessRecord.allocationRecord().allocation().getId(),
        createBusinessRecord.user());

    CreateUpdateUserRecord createdUser = testHelper.createUser(business);

    // Use the Business Owner email to demonstrate that no duplicate emails are permitted
    UpdateUserRequest userRecord =
        new UpdateUserRequest(
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
        createBusinessRecord.allocationRecord().allocation().getId(),
        createBusinessRecord.user());

    CreateUserRequest userRecord1 =
        new CreateUserRequest(
            faker.name().firstName(),
            faker.name().lastName(),
            new Address(testHelper.generateEntityAddress()),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            true);
    CreateUserRequest userRecord2 =
        new CreateUserRequest(
            faker.name().firstName(),
            faker.name().lastName(),
            new Address(testHelper.generateEntityAddress()),
            faker.internet().emailAddress(),
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
        business.getId(),
        "",
        createBusinessRecord.allocationRecord().allocation().getId(),
        createBusinessRecord.user());

    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "First",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber());
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
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

  void currentUser() {}

  void getUsersByUserName() {}

  @SneakyThrows
  @Test
  void getUserCards() {
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

      assertThat(cardDetailsResponse.getLimits())
          .containsOnly(new CurrencyLimit(Currency.USD, new HashMap<>()));

      assertThat(cardDetailsResponse.getDisabledMccGroups()).isEmpty();
      assertThat(cardDetailsResponse.getDisabledPaymentTypes()).isEmpty();
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

    assertThat(cardDetailsResponse.getLimits())
        .containsOnly(new CurrencyLimit(Currency.USD, new HashMap<>()));

    assertThat(cardDetailsResponse.getDisabledMccGroups()).isEmpty();
    assertThat(cardDetailsResponse.getDisabledPaymentTypes()).isEmpty();
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
  void retireCard() {
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
            "/users/cards/%s/retire".formatted(card.getId()),
            HttpMethod.PATCH,
            userCookie,
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            com.clearspend.capital.controller.type.card.Card.class);

    // then
    assertThat(blockedCard.getCardId()).isEqualTo(card.getId());
    assertThat(blockedCard.getStatus()).isEqualTo(CardStatus.CANCELLED);
    assertThat(blockedCard.getStatusReason()).isEqualTo(CardStatusReason.CARDHOLDER_REQUESTED);
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

    CardRecord cardRecord =
        cardService.issueCard(
            BinType.DEBIT,
            FundingType.POOLED,
            CardType.VIRTUAL,
            userRecord.user().getBusinessId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            userRecord.user().getId(),
            Currency.USD,
            true,
            createBusinessRecord.business().getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptySet(),
            Collections.emptySet(),
            createBusinessRecord.business().getClearAddress().toAddress());

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
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

    CardRecord cardRecord =
        cardService.issueCard(
            BinType.DEBIT,
            FundingType.POOLED,
            CardType.VIRTUAL,
            userRecord.user().getBusinessId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            userRecord.user().getId(),
            Currency.USD,
            true,
            createBusinessRecord.business().getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptySet(),
            Collections.emptySet(),
            createBusinessRecord.business().getClearAddress().toAddress());

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
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

    CardRecord cardRecord =
        cardService.issueCard(
            BinType.DEBIT,
            FundingType.POOLED,
            CardType.VIRTUAL,
            userRecord.user().getBusinessId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            userRecord.user().getId(),
            Currency.USD,
            true,
            createBusinessRecord.business().getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptySet(),
            Collections.emptySet(),
            createBusinessRecord.business().getClearAddress().toAddress());

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
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
            ExpenseCategoryStatus.ACTIVE);

    expenseCategoryRepository.save(expenseCategory);

    UpdateAccountActivityRequest updateAccountActivityRequest =
        new UpdateAccountActivityRequest("notesTest", expenseCategory.getId());

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

    CardRecord cardRecord =
        cardService.issueCard(
            BinType.DEBIT,
            FundingType.POOLED,
            CardType.VIRTUAL,
            userRecord.user().getBusinessId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            userRecord.user().getId(),
            Currency.USD,
            true,
            createBusinessRecord.business().getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptySet(),
            Collections.emptySet(),
            createBusinessRecord.business().getClearAddress().toAddress());

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
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
            ExpenseCategoryStatus.ACTIVE);

    expenseCategoryRepository.save(expenseCategory);

    UpdateAccountActivityRequest updateAccountActivityRequest =
        new UpdateAccountActivityRequest("notesTest", expenseCategory.getId());

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
    updateAccountActivityRequest = new UpdateAccountActivityRequest("notesTest", null);

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

    CardRecord cardRecord =
        cardService.issueCard(
            BinType.DEBIT,
            FundingType.POOLED,
            CardType.VIRTUAL,
            userRecord.user().getBusinessId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            userRecord.user().getId(),
            Currency.USD,
            true,
            createBusinessRecord.business().getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptySet(),
            Collections.emptySet(),
            createBusinessRecord.business().getClearAddress().toAddress());

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            userRecord.user(), cardRecord.card(), cardRecord.account(), amount);
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
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
            ExpenseCategoryStatus.ACTIVE);

    expenseCategoryRepository.save(expenseCategory);

    UpdateAccountActivityRequest updateAccountActivityRequest =
        new UpdateAccountActivityRequest("notesTest", expenseCategory.getId());

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

  void linkReceipt() {}

  void unlinkReceipt() {}

  void deleteReceipt() {}

  @SneakyThrows
  @Test
  void getUsersForBusinessIdByUserName() {
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createAllocation(
        business.getId(),
        "",
        createBusinessRecord.allocationRecord().allocation().getId(),
        createBusinessRecord.user());

    Cookie authCookie = testHelper.getDefaultAuthCookie();

    CreateUpdateUserRecord userRecord =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber());

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                get("/users/list")
                    .queryParam(
                        Common.USER_NAME,
                        userRecord.user().getFirstName().toString().substring(0, 3))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertTrue(
        objectMapper.readValue(responseFilteredByUserName.getContentAsString(), List.class).size()
            > 0);
    log.info(responseFilteredByUserName.getContentAsString());
  }

  @SneakyThrows
  @Test
  void searchForUsers() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.setCurrentUser(createBusinessRecord.user());
    AllocationService.AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
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
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    AllocationService.AllocationRecord allocation2 =
        testHelper.createAllocation(
            business.getId(),
            "allocation2",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    AllocationService.AllocationRecord allocation3 =
        testHelper.createAllocation(
            business.getId(),
            "allocation3",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    Cookie authCookie = createBusinessRecord.authCookie();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
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
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    AllocationService.AllocationRecord allocation2 =
        testHelper.createAllocation(
            business.getId(),
            "allocation2",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    AllocationService.AllocationRecord allocation3 =
        testHelper.createAllocation(
            business.getId(),
            "allocation3",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    Cookie authCookie = createBusinessRecord.authCookie();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
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

    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "First",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
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

  @SneakyThrows
  @Test
  void searchForUsersExcludeArchivedUsers() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
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

  @SneakyThrows
  @Test
  void searchForUsersIncludeArchivedUsers() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
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
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    AllocationService.AllocationRecord allocation2 =
        testHelper.createAllocation(
            business.getId(),
            "allocation2",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    Cookie authCookie = createBusinessRecord.authCookie();

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
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
}
