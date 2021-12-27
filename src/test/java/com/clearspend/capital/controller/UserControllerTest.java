package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.user.CreateUserRequest;
import com.clearspend.capital.controller.type.user.CreateUserResponse;
import com.clearspend.capital.controller.type.user.SearchUserRequest;
import com.clearspend.capital.controller.type.user.UpdateUserRequest;
import com.clearspend.capital.controller.type.user.UpdateUserResponse;
import com.clearspend.capital.controller.type.user.User;
import com.clearspend.capital.controller.type.user.UserPageData;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.model.enums.CardType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.NetworkMessageType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.ProgramService;
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
public class UserControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final CardService cardService;
  private final NetworkMessageService networkMessageService;
  private final UserService userService;
  private final ProgramService programService;

  private final Faker faker = new Faker();

  private Bin bin;
  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private Program program;
  private CreateUpdateUserRecord user;
  private Cookie userCookie;
  private Card card;
  private Card card2;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      user = testHelper.createUser(createBusinessRecord.business());
      userCookie = testHelper.login(user.user().getEmail().getEncrypted(), user.password());
      card =
          testHelper.issueCard(
              business,
              createBusinessRecord.allocationRecord().allocation(),
              user.user(),
              program,
              Currency.USD);
      card2 =
          testHelper.issueCard(
              business,
              createBusinessRecord.allocationRecord().allocation(),
              user.user(),
              program,
              Currency.USD);
    }
  }

  @BeforeEach
  void init() {
    testHelper.init();
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
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

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

    com.clearspend.capital.data.model.User user =
        userService.retrieveUsersForBusiness(business.getId()).stream()
            .filter(u -> u.getType() == UserType.EMPLOYEE)
            .findAny()
            .orElseThrow();
    Assertions.assertEquals(
        user.getId(),
        objectMapper
            .readValue(response.getContentAsString(), CreateUserResponse.class)
            .getUserId());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void updateUser() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

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
            createdUser.user().getEmail().toString(),
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

  @Test
  @SneakyThrows
  void bulkCreateUser() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

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
            faker.phoneNumber().phoneNumber(),
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
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

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
        faker.phoneNumber().phoneNumber());

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
      assertThat(cardDetailsResponse.getCard().getCardNumber())
          .isIn(card.getCardNumber().getEncrypted(), card2.getCardNumber().getEncrypted());

      assertThat(cardDetailsResponse.getAvailableBalance()).isNotNull();
      assertThat(cardDetailsResponse.getAvailableBalance().getCurrency())
          .isEqualTo(business.getCurrency());

      assertThat(cardDetailsResponse.getLedgerBalance()).isNotNull();
      assertThat(cardDetailsResponse.getLedgerBalance().getCurrency())
          .isEqualTo(business.getCurrency());

      assertThat(cardDetailsResponse.getLimits())
          .containsOnly(new CurrencyLimit(Currency.USD, new HashMap<>()));

      assertThat(cardDetailsResponse.getDisabledMccGroups()).isEmpty();
      assertThat(cardDetailsResponse.getDisabledTransactionChannels()).isEmpty();
    }
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
    assertThat(cardDetailsResponse.getCard().getCardNumber())
        .isEqualTo(card.getCardNumber().getEncrypted());

    assertThat(cardDetailsResponse.getAvailableBalance()).isNotNull();
    assertThat(cardDetailsResponse.getAvailableBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLedgerBalance()).isNotNull();
    assertThat(cardDetailsResponse.getLedgerBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLimits())
        .containsOnly(new CurrencyLimit(Currency.USD, new HashMap<>()));

    assertThat(cardDetailsResponse.getDisabledMccGroups()).isEmpty();
    assertThat(cardDetailsResponse.getDisabledTransactionChannels()).isEmpty();
  }

  void blockCard() {}

  void unblockCard() {}

  void retireCard() {}

  @SneakyThrows
  @Test
  void getCardAccountActivity() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    CreateUpdateUserRecord userRecord =
        userService.createUser(
            createBusinessRecord.business().getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());

    Cookie authCookie =
        testHelper.login(userRecord.user().getEmail().getEncrypted(), userRecord.password());

    final Program program = testHelper.retrievePooledProgram();
    CardRecord cardRecord =
        cardService.issueCard(
            program,
            userRecord.user().getBusinessId(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            userRecord.user().getId(),
            Currency.USD,
            true,
            createBusinessRecord.business().getLegalName(),
            Map.of(Currency.USD, new HashMap<>()),
            Collections.emptyList(),
            Collections.emptySet());

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    networkMessageService.processNetworkMessage(
        TestDataController.generateNetworkCommon(
            NetworkMessageType.PRE_AUTH,
            userRecord.user(),
            cardRecord.card(),
            cardRecord.account(),
            program,
            amount));

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

  void getAccountActivity() {}

  void updateAccountActivity() {}

  void linkReceipt() {}

  void unlinkReceipt() {}

  @SneakyThrows
  @Test
  void getUsersForBusinessIdByUserName() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    testHelper.createBin();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.createBusinessOwner(createBusinessRecord.business().getId(), email, password);
    Business business = createBusinessRecord.business();
    testHelper.createAllocation(
        business.getId(),
        "",
        createBusinessRecord.allocationRecord().allocation().getId(),
        createBusinessRecord.user());

    Cookie authCookie = testHelper.login(email, password);

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
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    testHelper.createBin();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.createBusinessOwner(createBusinessRecord.business().getId(), email, password);
    Business business = createBusinessRecord.business();
    AllocationService.AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    Cookie authCookie = testHelper.login(email, password);

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
        testHelper.retrievePooledProgram(),
        Currency.USD);
    testHelper.issueCard(
        business,
        allocation.allocation(),
        user.user(),
        testHelper.retrievePooledProgram(),
        Currency.USD);
    testHelper.issueCard(
        business,
        allocation.allocation(),
        user1.user(),
        testHelper.retrievePooledProgram(),
        Currency.USD);

    SearchUserRequest searchUserRequest = new SearchUserRequest();
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
    Assertions.assertEquals(4, userPageData.getTotalElements());
  }

  @SneakyThrows
  @Test
  void searchForUsersFilteredByAllocation() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    testHelper.createBin();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.createBusinessOwner(createBusinessRecord.business().getId(), email, password);
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

    Cookie authCookie = testHelper.login(email, password);

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
        testHelper.retrievePooledProgram(),
        Currency.USD);
    testHelper.issueCard(
        business,
        allocation2.allocation(),
        user.user(),
        testHelper.retrievePooledProgram(),
        Currency.USD);
    testHelper.issueCard(
        business,
        allocation3.allocation(),
        user1.user(),
        testHelper.retrievePooledProgram(),
        Currency.USD);

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
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    testHelper.createBin();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.createBusinessOwner(createBusinessRecord.business().getId(), email, password);
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

    Cookie authCookie = testHelper.login(email, password);

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
        testHelper.retrievePooledProgram(),
        Currency.USD);
    testHelper.issueCard(
        business,
        allocation2.allocation(),
        user.user(),
        testHelper.retrieveIndividualProgram(),
        Currency.USD);
    testHelper.issueCard(
        business,
        allocation3.allocation(),
        user1.user(),
        testHelper.retrievePooledProgram(),
        Currency.USD);

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
    Assertions.assertEquals(1, userPageData.getTotalElements());
    Assertions.assertTrue(
        userPageData.getContent().get(0).getCardInfoList().stream()
            .anyMatch(
                cardInfo ->
                    programService
                        .retrieveProgram(
                            cardService
                                .retrieveCard(business.getId(), cardInfo.getCardId())
                                .getProgramId())
                        .getCardType()
                        .equals(CardType.PLASTIC)));
  }

  @SneakyThrows
  @Test
  void searchForUsersFilteredBySmartSearch() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    testHelper.createBin();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.createBusinessOwner(createBusinessRecord.business().getId(), email, password);
    Business business = createBusinessRecord.business();

    Cookie authCookie = testHelper.login(email, password);

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

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setSearchText("Name");
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
    Assertions.assertEquals("Name", userPageData.getContent().get(0).getUserData().getFirstName());
  }

  @SneakyThrows
  @Test
  void searchForUsersExcludeArchivedUsers() {
    testHelper.createBin();
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
    testHelper.createBin();
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
}