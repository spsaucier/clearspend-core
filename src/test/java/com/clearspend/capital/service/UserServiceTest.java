package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.github.javafaker.Faker;
import javax.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
class UserServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  private final Faker faker = new Faker();

  @Autowired private UserRepository userRepository;

  @Autowired private UserService userService;

  private CreateBusinessRecord createBusinessRecord;
  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
      createBusinessRecord = testHelper.createBusiness();
    }
  }

  @SneakyThrows
  @Test
  void createUser() {
    CreateUpdateUserRecord userRecord = testHelper.createUser(createBusinessRecord.business());
    User foundUser = userRepository.findById(userRecord.user().getId()).orElseThrow();
    assertThat(foundUser).isNotNull();
  }

  @SneakyThrows
  @Test
  void createUser_withoutAddress() {
    CreateUpdateUserRecord userRecord =
        userService.createUser(
            createBusinessRecord.business().getId(),
            UserType.EMPLOYEE,
            faker.name().firstName(),
            faker.name().lastName(),
            null,
            faker.internet().emailAddress(),
            faker.phoneNumber().phoneNumber());
    User foundUser = userRepository.findById(userRecord.user().getId()).orElseThrow();
    assertThat(foundUser).isNotNull();
  }

  @SneakyThrows
  @Test
  void createUser_withoutPhone() {
    CreateUpdateUserRecord userRecord =
        userService.createUser(
            createBusinessRecord.business().getId(),
            UserType.EMPLOYEE,
            faker.name().firstName(),
            faker.name().lastName(),
            null,
            faker.internet().emailAddress(),
            null);
    User foundUser = userRepository.findById(userRecord.user().getId()).orElseThrow();
    assertThat(foundUser).isNotNull();
  }
}
