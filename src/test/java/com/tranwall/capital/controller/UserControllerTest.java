package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javafaker.Faker;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.UserService;
import com.tranwall.capital.service.UserService.CreateUserRecord;
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

  private Cookie authCookie;

  @BeforeEach
  void init() {
    testHelper.init();
    this.authCookie = testHelper.login("tester@tranwall.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void getUsers() {
    Bin bin = testHelper.createBin();
    Program program = testHelper.createProgram(bin);
    Business business = testHelper.createBusiness(program).business();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), "", null);

    CreateUserRecord userRecord =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            true);
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        true);

    MockHttpServletResponse response =
        mvc.perform(
                get("/users/list")
                    .header("businessId", business.getId().toString())
                    .contentType("application/json")
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
  void getUsersForBusinessIdByUserName() {
    Bin bin = testHelper.createBin();
    Program program = testHelper.createProgram(bin);
    Business business = testHelper.createBusiness(program).business();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), "", null);

    CreateUserRecord userRecord =
        userService.createUser(
            business.getId(),
            UserType.EMPLOYEE,
            "First",
            "Last",
            testHelper.generateEntityAddress(),
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber(),
            true);
    userService.createUser(
        business.getId(),
        UserType.EMPLOYEE,
        "Name",
        "Last",
        testHelper.generateEntityAddress(),
        faker.internet().emailAddress(),
        faker.phoneNumber().phoneNumber(),
        true);

    MockHttpServletResponse responseFilteredByUserName =
        mvc.perform(
                get("/users/list")
                    .header("businessId", business.getId().toString())
                    .queryParam(
                        UserController.USER_NAME,
                        userRecord.user().getFirstName().toString().substring(0, 3))
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
}
