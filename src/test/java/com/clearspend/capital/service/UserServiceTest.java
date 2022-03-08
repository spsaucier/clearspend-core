package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.github.javafaker.Faker;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class UserServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  private final Faker faker = new Faker();

  @Autowired private UserRepository userRepository;

  @Autowired private UserService userService;

  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
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

  @SneakyThrows
  @Test
  void testForEmailUniquenessWhenUpdatingUserRecords() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User existingUser =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    User toChange =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    // Ensure we have different email addresses to start
    assertThat(existingUser.getEmail()).isNotEqualTo(toChange.getEmail());

    assertThrows(
        InvalidRequestException.class,
        () ->
            userService.updateUser(
                createBusinessRecord.business().getId(),
                toChange.getId(),
                toChange.getFirstName().toString(),
                toChange.getLastName().toString(),
                toChange.getAddress(),
                existingUser
                    .getEmail()
                    .toString(), // This should cause an Exception when update is invoked
                toChange.getPhone().toString(),
                false));

    // Ensure we have different email addresses in the end
    assertThat(existingUser.getEmail())
        .isNotEqualTo(userService.retrieveUser(toChange.getId()).getEmail());
  }
}
