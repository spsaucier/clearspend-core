package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javafaker.Faker;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
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
            faker.phoneNumber().phoneNumber(),
            true,
            null);
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
            null,
            true,
            null);
    User foundUser = userRepository.findById(userRecord.user().getId()).orElseThrow();
    assertThat(foundUser).isNotNull();
  }
}
