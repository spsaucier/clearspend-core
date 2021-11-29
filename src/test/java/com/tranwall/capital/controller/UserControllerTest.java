package com.tranwall.capital.controller;

import static com.tranwall.capital.controller.Common.USER_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javafaker.Faker;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.controller.nonprod.TestDataController;
import com.tranwall.capital.controller.type.Address;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.controller.type.user.CreateUserRequest;
import com.tranwall.capital.controller.type.user.CreateUserResponse;
import com.tranwall.capital.controller.type.user.SearchUserRequest;
import com.tranwall.capital.controller.type.user.UpdateUserRequest;
import com.tranwall.capital.controller.type.user.UpdateUserResponse;
import com.tranwall.capital.controller.type.user.User;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.NetworkMessageType;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.AllocationService;
import com.tranwall.capital.service.CardService;
import com.tranwall.capital.service.CardService.CardRecord;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.Cookie;
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
// @Transactional
public class UserControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final CardService cardService;
  private final NetworkMessageService networkMessageService;
  private final UserService userService;

  private final Faker faker = new Faker();

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
        createBusinessRecord.allocationRecord().allocation().getId());

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

    com.tranwall.capital.data.model.User user =
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
        createBusinessRecord.allocationRecord().allocation().getId());

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

    com.tranwall.capital.data.model.User user =
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
        createBusinessRecord.allocationRecord().allocation().getId());

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
        business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());

    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "First",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        true,
        null);
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        true,
        null);

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

  void getUserCards() {}

  void getUserCard() {}

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
            faker.phoneNumber().phoneNumber(),
            true,
            null);

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
            createBusinessRecord.business().getLegalName());

    Amount amount = Amount.of(Currency.USD, BigDecimal.ONE);
    networkMessageService.processNetworkMessage(
        TestDataController.generateNetworkCommon(
            NetworkMessageType.PRE_AUTH_TRANSACTION,
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
        business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());

    Cookie authCookie = testHelper.login(email, password);

    CreateUpdateUserRecord userRecord =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            true,
            null);
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        true,
        null);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                get("/users/list")
                    .queryParam(
                        USER_NAME, userRecord.user().getFirstName().toString().substring(0, 3))
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
            business.getId(), "", createBusinessRecord.allocationRecord().allocation().getId());

    Cookie authCookie = testHelper.login(email, password);

    CreateUpdateUserRecord user =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            true,
            null);
    CreateUpdateUserRecord user1 =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "Name",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            true,
            null);

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
  }
}
