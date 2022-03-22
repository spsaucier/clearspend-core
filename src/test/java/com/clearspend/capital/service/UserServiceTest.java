package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.github.javafaker.Faker;
import com.stripe.model.Person;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
class UserServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  private final Faker faker = new Faker();

  @Autowired private UserRepository userRepository;

  @Autowired private UserService userService;

  @Autowired private StripeMockClient stripeMockClient;

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
  void createUser_exceptionThrownWhenCreatingUserWithNullEmailAddress() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    assertThrows(
        NullPointerException.class,
        () ->
            userService.createUser(
                createBusinessRecord.business().getId(),
                UserType.EMPLOYEE,
                faker.name().firstName(),
                faker.name().lastName(),
                testHelper.generateEntityAddress(),
                null, // MISSING EMAIL ADDRESS
                faker.phoneNumber().phoneNumber()));
  }

  @SneakyThrows
  @Test
  void createUser_exceptionThrownWhenCreatingUserWithEmptyEmailAddress() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            userService.createUser(
                createBusinessRecord.business().getId(),
                UserType.EMPLOYEE,
                faker.name().firstName(),
                faker.name().lastName(),
                testHelper.generateEntityAddress(),
                "", // EMPTY EMAIL ADDRESS
                faker.phoneNumber().phoneNumber()));
  }

  @SneakyThrows
  @Test
  void createUser_exceptionThrownWhenCreatingUserWithDuplicateEmailAddress() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateBusinessRecord newBusiness = testHelper.createBusiness();
    String emailAddress = faker.internet().emailAddress();

    // Create the first Employee on the createBusinessRecord.business
    userService.createUser(
        createBusinessRecord.business().getId(),
        UserType.EMPLOYEE,
        faker.name().firstName(),
        faker.name().lastName(),
        testHelper.generateEntityAddress(),
        emailAddress,
        faker.phoneNumber().phoneNumber());

    // Ensure that another User with the same email address cannot be created, even on another
    // Business
    testHelper.setCurrentUser(newBusiness.user());
    assertThrows(
        InvalidRequestException.class,
        () ->
            userService.createUser(
                newBusiness.business().getId(),
                UserType.EMPLOYEE,
                faker.name().firstName(),
                faker.name().lastName(),
                testHelper.generateEntityAddress(),
                emailAddress,
                faker.phoneNumber().phoneNumber()));
  }

  @SneakyThrows
  @Test
  void testUpdatingUserNameAndAddress() {
    User user = createBusinessRecord.user();
    testHelper.setCurrentUser(user);

    String newFirstName = testHelper.generateFirstName();
    String newLastName = testHelper.generateLastName();
    Address newAddress = testHelper.generateEntityAddress();
    long stripePersonWrites = stripeMockClient.countCreatedObjectsByType(Person.class);
    userService.updateUser(
        user.getBusinessId(),
        user.getId(),
        newFirstName,
        newLastName,
        newAddress,
        user.getEmail().getEncrypted(),
        user.getPhone().getEncrypted(),
        false);

    // Check the change landed in the Users table
    User revisedUser = userService.retrieveUser(user.getId());
    assertThat(revisedUser.getFirstName().getEncrypted()).isEqualTo(newFirstName);
    assertThat(revisedUser.getLastName().getEncrypted()).isEqualTo(newLastName);
    assertThat(revisedUser.getAddress()).isEqualTo(newAddress);

    // Names and addresses aren't in FusionAuth, so skipping that

    // Check the change went to Stripe
    assertThat(stripeMockClient.countCreatedObjectsByType(Person.class))
        .isEqualTo(stripePersonWrites + 1);
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

    // We should be able to update other fields on the User without triggering the Email duplication
    // message
    String emailPriorToUpdate = toChange.getEmail().toString();
    assertThat(
            userService
                .updateUser(
                    createBusinessRecord.business().getId(),
                    toChange.getId(),
                    toChange.getFirstName().toString(),
                    faker.name().lastName(),
                    toChange.getAddress(),
                    toChange.getEmail().toString(),
                    toChange.getPhone().toString(),
                    false)
                .user()
                .getEmail()
                .toString())
        .isEqualTo(emailPriorToUpdate);
  }
}
