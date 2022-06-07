package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.controller.type.card.CardAllocationSpendControls;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.user.UpdateUserRequest;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.UserRepositoryCustom.FilteredUserWithCardListRecord;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUser;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.github.javafaker.Faker;
import com.stripe.model.Person;
import com.stripe.model.issuing.Cardholder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;

@Slf4j
class UserServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  private final Faker faker = new Faker();

  @Autowired private UserRepository userRepository;

  @Autowired private UserService userService;
  @Autowired private PermissionValidationHelper permissionValidationHelper;

  @Autowired private StripeMockClient stripeMockClient;

  @Autowired private CardService cardService;
  @Autowired private FusionAuthService fusionAuthService;

  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
  }

  @AfterEach
  void cleanup() {
    stripeMockClient.reset();
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
            testHelper.createRandomEmail(),
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
            testHelper.createRandomEmail(),
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
    String emailAddress = testHelper.createRandomEmail();

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
  void testUpdatingOwnerNameAndAddress() {
    User user = createBusinessRecord.user();
    testHelper.setCurrentUser(user);

    changeNameAndAddress(user, Person.class);
  }

  @SneakyThrows
  @Test
  void testUpdatingEmployeeNameAndAddress() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();

    final CardAllocationSpendControls controls =
        CardAllocationSpendControls.of(createBusinessRecord.allocationRecord().allocation());
    controls.setLimits(CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())));
    controls.setDisabledMccGroups(Set.of());
    controls.setDisabledPaymentTypes(Set.of());
    controls.setDisableForeign(false);

    final IssueCardRequest issueCardRequest =
        new IssueCardRequest(Set.of(), employee.getId(), Currency.USD, true, List.of(controls));
    issueCardRequest.setFundingType(FundingType.POOLED);
    issueCardRequest.setShippingAddress(
        new com.clearspend.capital.controller.type.Address(employee.getAddress()));

    CardRecord cardRec =
        cardService.issueCard(
            createBusinessRecord.business().getId(), CardType.VIRTUAL, issueCardRequest);

    changeNameAndAddress(employee, Cardholder.class);
  }

  private void changeNameAndAddress(User user, Class<?> clazz) {
    user.setExternalRef(UUID.randomUUID().toString().substring(0, 30));
    userRepository.saveAndFlush(user);
    String newFirstName = testHelper.generateFirstName();
    String newLastName = testHelper.generateLastName();
    com.clearspend.capital.controller.type.Address newAddress = testHelper.generateApiAddress();
    long stripePersonWrites = stripeMockClient.countCreatedObjectsByType(clazz);
    userService.updateUser(
        new UpdateUserRequest(
            user.getId(),
            user.getBusinessId(),
            newFirstName,
            newLastName,
            newAddress,
            user.getEmail().getEncrypted(),
            user.getPhone().getEncrypted(),
            false));

    // Check the change landed in the Users table
    User revisedUser = userService.retrieveUser(user.getId());
    assertThat(revisedUser.getFirstName().getEncrypted()).isEqualTo(newFirstName);
    assertThat(revisedUser.getLastName().getEncrypted()).isEqualTo(newLastName);
    assertThat(revisedUser.getAddress()).isEqualTo(newAddress.toAddress());

    // Names and addresses aren't in FusionAuth, so skipping that

    // Check the change went to Stripe
    assertThat(stripeMockClient.getCreatedObject(user.getExternalRef())).isNotNull();
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
                new UpdateUserRequest(
                    toChange.getId(),
                    createBusinessRecord.business().getId(),
                    toChange.getFirstName().toString(),
                    toChange.getLastName().toString(),
                    new Address(toChange.getAddress()),
                    existingUser
                        .getEmail()
                        .toString(), // This should cause an Exception when update is invoked
                    toChange.getPhone().toString(),
                    false)));

    // Ensure we have different email addresses in the end
    assertThat(existingUser.getEmail())
        .isNotEqualTo(userService.retrieveUser(toChange.getId()).getEmail());

    // We should be able to update other fields on the User without triggering the Email duplication
    // message
    String emailPriorToUpdate = toChange.getEmail().toString();
    assertThat(
            userService
                .updateUser(
                    new UpdateUserRequest(
                        toChange.getId(),
                        createBusinessRecord.business().getId(),
                        toChange.getFirstName().toString(),
                        faker.name().lastName(),
                        new Address(toChange.getAddress()),
                        toChange.getEmail().toString(),
                        toChange.getPhone().toString(),
                        false))
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
                testHelper.createRandomEmail(),
                "123456789");
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void updateUser_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User existing =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final ThrowingRunnable action =
        () ->
            userService.updateUser(
                new UpdateUserRequest(
                    existing.getId(),
                    createBusinessRecord.business().getId(),
                    "Bob",
                    "Saget",
                    null,
                    testHelper.createRandomEmail(),
                    "123456789",
                    false));
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void updateUser_getsDistributed_CAP_908() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    User existing =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    testHelper.issueCard(
        createBusinessRecord.business(),
        createBusinessRecord.allocationRecord().allocation(),
        existing,
        Currency.USD,
        FundingType.POOLED,
        CardType.VIRTUAL,
        false);

    assertThat(fusionAuthService.getUser(FusionAuthUser.fromUser(existing)).email)
        .isEqualTo(existing.getEmail().getEncrypted());
    long cardholderObjs = stripeMockClient.countCreatedObjectsByType(Cardholder.class);
    testHelper.setCurrentUser(createBusinessRecord.user());
    existing =
        userService
            .updateUser(
                new UpdateUserRequest(
                    existing.getId(),
                    createBusinessRecord.business().getId(),
                    "Bob",
                    "Saget",
                    null,
                    testHelper.createRandomEmail(),
                    "123456789",
                    false))
            .user();

    assertThat(fusionAuthService.getUser(FusionAuthUser.fromUser(existing)).email)
        .isEqualTo(existing.getEmail().getEncrypted());
    assertThat(stripeMockClient.getCreatedObject(existing.getExternalRef())).isNotNull();
  }

  @Test
  void updateUser_UserPermissions_existingUser() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final User existing =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final ThrowingRunnable action =
        () ->
            userService.updateUser(
                new UpdateUserRequest(
                    existing.getId(),
                    createBusinessRecord.business().getId(),
                    null,
                    null,
                    null,
                    null,
                    "123456789",
                    false));
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .allowUser(existing)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  @SneakyThrows
  void retrieveUsersForBusiness_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Allocation child =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child",
                createBusinessRecord.allocationRecord().allocation().getId())
            .allocation();
    final User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final ThrowingSupplier<List<User>> action =
        () -> userService.retrieveUsersForBusiness(createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(child)
        .<List<User>>allowRolesOnAllocationWithResult(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY),
            records ->
                // Will contain all users created for the test, so using these two to ensure it's
                // working
                assertThat(records).contains(createBusinessRecord.user(), employee))
        .<List<User>>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_EMPLOYEE,
            records ->
                // Should only contain the user created with the role, but we don't have that
                // reference here
                assertThat(records)
                    .hasSize(1)
                    .doesNotContain(createBusinessRecord.user(), employee))
        .<List<User>>allowUserWithResult(
            employee, records -> assertThat(records).hasSize(1).contains(employee))
        .<List<User>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER),
            records -> assertThat(records).hasSizeGreaterThan(1))
        .<List<User>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_VIEWER,
                DefaultRoles.GLOBAL_BOOKKEEPER,
                DefaultRoles.GLOBAL_RESELLER),
            records ->
                // Do not have VIEW_OWN permission so can't see anything
                assertThat(records).isEmpty())
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void retrieveUserPage_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Allocation child =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child",
                createBusinessRecord.allocationRecord().allocation().getId())
            .allocation();

    final User user1 = testHelper.createUser(createBusinessRecord.business()).user();
    final User user2 = testHelper.createUser(createBusinessRecord.business()).user();
    final User user3 = testHelper.createUser(createBusinessRecord.business()).user();

    final PageRequest pageRequest = new PageRequest();
    pageRequest.setPageSize(50);
    pageRequest.setPageNumber(0);
    final UserFilterCriteria criteria = new UserFilterCriteria();
    criteria.setPageToken(PageRequest.toPageToken(pageRequest));
    final ThrowingSupplier<Page<FilteredUserWithCardListRecord>> action =
        () -> userService.retrieveUserPage(createBusinessRecord.business().getId(), criteria);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(child)
        // These users should see all users, including the four created above
        .<Page<FilteredUserWithCardListRecord>>allowRolesOnAllocationWithResult(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY),
            records ->
                assertThat(toUsers(records))
                    .hasSizeGreaterThan(4)
                    .contains(createBusinessRecord.user(), user1, user2, user3))
        // This user should only see themselves
        .<Page<FilteredUserWithCardListRecord>>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_EMPLOYEE, records -> assertThat(toUsers(records)).hasSize(1))
        // These users should see all users, including the four created above
        .<Page<FilteredUserWithCardListRecord>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER),
            records ->
                assertThat(toUsers(records))
                    .hasSizeGreaterThan(4)
                    .contains(createBusinessRecord.user(), user1, user2, user3))
        // These users should see only themselves
        .<Page<FilteredUserWithCardListRecord>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_BOOKKEEPER,
                DefaultRoles.GLOBAL_VIEWER,
                DefaultRoles.GLOBAL_RESELLER),
            records -> assertThat(toUsers(records)).isEmpty())
        .build()
        .validateServiceMethod(action);
  }

  private List<User> toUsers(final Page<FilteredUserWithCardListRecord> page) {
    return page.get().map(FilteredUserWithCardListRecord::user).toList();
  }

  @Test
  void retrieveUser_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Allocation child =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child",
                createBusinessRecord.allocationRecord().allocation().getId())
            .allocation();
    final User owner =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final ThrowingRunnable action = () -> userService.retrieveUser(owner.getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .setAllocation(child)
        .allowRolesOnAllocation(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .allowUser(owner)
        .build()
        .validateServiceMethod(action);
  }
}
