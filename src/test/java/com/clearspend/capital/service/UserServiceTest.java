package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.testutils.permission.CustomUser;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.github.javafaker.Faker;
import com.stripe.model.Person;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
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
  @Autowired private PermissionValidationHelper permissionValidationHelper;

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
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord userRecord = testHelper.createUser(createBusinessRecord.business());
    User foundUser = userRepository.findById(userRecord.user().getId()).orElseThrow();
    assertThat(foundUser).isNotNull();
  }

  @SneakyThrows
  @Test
  void createUser_withoutAddress() {
    testHelper.setCurrentUser(createBusinessRecord.user());
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
    testHelper.setCurrentUser(createBusinessRecord.user());
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

    testHelper.setCurrentUser(createBusinessRecord.user());

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

  @Test
  void createUser_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final ThrowingRunnable action =
        () ->
            userService.createUser(
                createBusinessRecord.business().getId(),
                UserType.EMPLOYEE,
                "Bob",
                "Saget",
                null,
                "bs@clearspend.com",
                "123456789");
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void updateUser_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User existing = testHelper.createUser(createBusinessRecord.business()).user();
    final ThrowingRunnable action =
        () ->
            userService.updateUser(
                createBusinessRecord.business().getId(),
                existing.getId(),
                "Bob",
                "Saget",
                null,
                "bs@clearspend.com",
                "123456789",
                false);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void retrieveUsersForBusiness_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final ThrowingRunnable action =
        () -> userService.retrieveUsersForBusiness(createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void retrieveUserPage_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final PageRequest pageRequest = new PageRequest();
    pageRequest.setPageSize(1);
    pageRequest.setPageNumber(1);
    final UserFilterCriteria criteria = new UserFilterCriteria();
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));
    final ThrowingRunnable action =
        () -> userService.retrieveUserPage(createBusinessRecord.business().getId(), criteria);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void archiveUser_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User archive = testHelper.createUser(createBusinessRecord.business()).user();
    final ThrowingRunnable action =
        () -> userService.archiveUser(createBusinessRecord.business().getId(), archive.getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void createCSVFile_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final PageRequest pageRequest = new PageRequest();
    pageRequest.setPageSize(1);
    pageRequest.setPageNumber(1);
    final UserFilterCriteria criteria = new UserFilterCriteria();
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));
    final ThrowingRunnable action =
        () -> userService.createCSVFile(createBusinessRecord.business().getId(), criteria);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void retrieveUser_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User owner =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final ThrowingRunnable action = () -> userService.retrieveUser(owner.getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .addRootAllocationCustomUser(CustomUser.pass(owner))
        .build()
        .validateServiceMethod(action);
  }
}
