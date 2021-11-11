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
import com.tranwall.capital.controller.type.Address;
import com.tranwall.capital.controller.type.common.PageRequest;
import com.tranwall.capital.controller.type.user.CreateUserRequest;
import com.tranwall.capital.controller.type.user.CreateUserResponse;
import com.tranwall.capital.controller.type.user.SearchUserRequest;
import com.tranwall.capital.controller.type.user.UpdateUserRequest;
import com.tranwall.capital.controller.type.user.UpdateUserResponse;
import com.tranwall.capital.controller.type.user.User;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
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
public class UserControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
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
    Bin bin = testHelper.createBin();
    Program program = testHelper.createProgram(bin);
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        businessAndAllocationsRecord.allocationRecord().allocation().getId());

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
            .get();
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
    Bin bin = testHelper.createBin();
    Program program = testHelper.createProgram(bin);
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    testHelper.createAllocation(
        business.getId(),
        "allocationName",
        businessAndAllocationsRecord.allocationRecord().allocation().getId());

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

  @SneakyThrows
  @Test
  void getUsers() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    Bin bin = testHelper.createBin();
    Program program = testHelper.createProgram(bin);
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    Business business = businessAndAllocationsRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    testHelper.createAllocation(
        business.getId(), "", businessAndAllocationsRecord.allocationRecord().allocation().getId());

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

  @SneakyThrows
  @Test
  void getUsersForBusinessIdByUserName() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    testHelper.createBin();
    Program program = testHelper.retrievePooledProgram();
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    testHelper.createBusinessOwner(
        businessAndAllocationsRecord.business().getId(), email, password);
    Business business = businessAndAllocationsRecord.business();
    testHelper.createAllocation(
        business.getId(), "", businessAndAllocationsRecord.allocationRecord().allocation().getId());

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
                    .header("businessId", business.getId().toString())
                    .queryParam(
                        USER_NAME, userRecord.user().getFirstName().toString().substring(0, 3))
                    .contentType("application/json")
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    Assertions.assertEquals(
        1,
        objectMapper.readValue(responseFilteredByUserName.getContentAsString(), List.class).size());
    log.info(responseFilteredByUserName.getContentAsString());
  }

  @SneakyThrows
  @Test
  void searchForUsers() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    testHelper.createBin();
    Program program = testHelper.retrievePooledProgram();
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    testHelper.createBusinessOwner(
        businessAndAllocationsRecord.business().getId(), email, password);
    Business business = businessAndAllocationsRecord.business();
    testHelper.createAllocation(
        business.getId(), "", businessAndAllocationsRecord.allocationRecord().allocation().getId());

    Cookie authCookie = testHelper.login(email, password);

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

    SearchUserRequest searchUserRequest = new SearchUserRequest();
    searchUserRequest.setPageRequest(PageRequest.builder().pageNumber(0).pageSize(10).build());

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
