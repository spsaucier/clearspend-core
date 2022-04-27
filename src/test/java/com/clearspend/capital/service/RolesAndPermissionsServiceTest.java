package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestAppender;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher.SwitchesCurrentUser;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.model.security.UserAllocationRole;
import com.clearspend.capital.data.repository.security.AllocationRolePermissionsRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.clearspend.capital.service.FusionAuthService.FusionAuthRoleAdministrator;
import com.clearspend.capital.service.FusionAuthService.RoleChange;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.type.CurrentUser;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class RolesAndPermissionsServiceTest extends BaseCapitalTest implements DefaultRoles {

  private static final BigDecimal TWO = new BigDecimal(2);

  @Autowired private TestHelper testHelper;
  @Autowired AllocationService allocationService;
  @Autowired UserAllocationRoleRepository userAllocationRoleRepository;
  @Autowired AllocationRolePermissionsRepository allocationRolePermissionsRepo;
  @Autowired RolesAndPermissionsService rolesAndPermissionsService;
  @Autowired UserService userService;
  @Autowired BusinessBankAccountService businessBankAccountService;
  @Autowired EntityManager entityManager;
  @Autowired FusionAuthService fusionAuthService;

  private CreateBusinessRecord createBusinessRecord;
  private Allocation rootAllocation;
  private User rootAllocationOwner;
  private TestAppender testAppender;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.createBusiness();
    rootAllocation = createBusinessRecord.allocationRecord().allocation();
    rootAllocationOwner = entityManager.getReference(User.class, rootAllocation.getOwnerId());
    testHelper.setCurrentUser(createBusinessRecord.user());

    // Give the root allocation some money
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId()).getId();
    businessBankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccountId,
        createBusinessRecord.user().getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("720.51")),
        false);

    // Capturing the log
    testAppender = TestAppender.watching(RolesAndPermissionsService.class, Level.INFO);
  }

  @AfterEach
  void tearDown() {
    testAppender.close();
  }

  /* All these commented-out tests will come out as their assertions are refactored
    to fit the newer paradigm.
  */
  /*
        // test if a user is an allocation owner with no permissions (=manage)
        @Test
        void allocationOwnerHasManagePermissionOnTheAllocation() {

          @NonNull User allocationOwner = rootAllocation.getOwner();

          // As a precondition of the test, the user's permissions must not have been
          // initialized.  (We may migrate, or make all owners managers by default, but for now...)
          assertFalse(
              allocationRoleRepository
                  .findByUserIdAndAllocationId(allocationOwner.getId(), rootAllocation.getId())
                  .isPresent());

          Stream.of(
                  AllocationPermission.NONE,
                  AllocationPermission.READ,
                  AllocationPermission.CODE,
                  AllocationPermission.MANAGE)
              .forEach(
                  permission ->
                      roleService.assertUserHasRole(
                          rootAllocation.getBusinessId(),
                          allocationOwner.getId(),
                          rootAllocation.getId(),
                          permission));

          assertThrows(
              ForbiddenException.class,
              () ->
                  roleService.assertUserHasRole(
                      rootAllocation.getBusinessId(),
                      allocationOwner.getId(),
                      rootAllocation.getId(),
                      AllocationPermission.CUSTOMER_SERVICE));
        }
  */

  /*
  // test if a user has permission on an allocation
  @Test
  void testNonAllocationOwnerPermissionsForAnAllocation() {
    User grantee = testHelper.createUser(createBusinessRecord.business()).user();
    User grantee2 = testHelper.createUser(createBusinessRecord.business()).user();
    User allocationOwner = rootAllocation.getOwner();

    // No permissions before granting them
    assertUserLacksPermission(
        grantee, rootAllocation, EnumSet.of(AllocationPermission.MANAGE_PERMISSIONS));

    // Someone has to grant permissions
    rolesAndPermissionsService.createUpdateAllocationUserRole(
        allocationOwner, grantee, rootAllocation, EnumSet.of(AllocationPermission.CODE));

    assertUserHasPermission(grantee, rootAllocation, EnumSet.of(AllocationPermission.CODE));

    // test elevating user permissions
    rolesAndPermissionsService.createUpdateAllocationUserRole(
        allocationOwner, grantee, rootAllocation, AllocationPermission.ALLOCATION_OWNER_ROLE);

    assertUserHasPermission(grantee, rootAllocation, AllocationPermission.ALLOCATION_OWNER_ROLE);

    // The grantee does not have all the permissions the root owner does, so
    // cannot give those permissions
    assertThrows(
        ForbiddenException.class,
        () ->
            rolesAndPermissionsService.createUpdateAllocationUserRole(
                grantee,
                grantee2,
                rootAllocation,
                AllocationPermission.ROOT_ALLOCATION_OWNER_ROLE));

    // But grantee can add permissions they have
    rolesAndPermissionsService.createUpdateAllocationUserRole(
        grantee,
        grantee2,
        rootAllocation,
        EnumSet.of(
            AllocationPermission.READ,
            AllocationPermission.CODE,
            AllocationPermission.LINK_RECEIPTS));

    // And if grantee doesn't have the MANAGE_PERMISSIONS permission, that won't work, either
    rolesAndPermissionsService.deleteAllocationUserRole(
        allocationOwner,
        grantee,
        rootAllocation,
        EnumSet.of(AllocationPermission.MANAGE_PERMISSIONS));
    assertThrows(
        ForbiddenException.class,
        () ->
            rolesAndPermissionsService.createUpdateAllocationUserRole(
                grantee, grantee2, rootAllocation, EnumSet.of(AllocationPermission.MANAGE_USERS)));

    // test lowering user permission
    rolesAndPermissionsService.setAllocationRoles(
        allocationOwner, grantee, rootAllocation, EnumSet.of(AllocationPermission.READ));

    assertUserHasPermission(grantee, rootAllocation, EnumSet.of(AllocationPermission.READ));
  }
  */

  // test for an error lowering permission of an allocation owner below Manager
  @Test
  void testNoLoweringAllocationOwnerPermission() {
    CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());

    // Default user (business owner) has Admin permission
    setCurrentUser(rootAllocationOwner);
    assertEquals(
        ALLOCATION_ADMIN,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(rootAllocation.getId())
            .allocationRole());

    // The allocation owner gives someone else manage permission
    User otherUser = newUser.user();
    setCurrentUser(rootAllocationOwner);
    UserAllocationRole userAllocationRole =
        rolesAndPermissionsService.createOrUpdateUserAllocationRole(
            otherUser, rootAllocation, ALLOCATION_MANAGER);

    assertThat(
            testAppender.countEventsForLogger(
                "com.clearspend.capital.service.RolesAndPermissionsService"))
        .isEqualTo(3);
    assertThat(testAppender.contains("User's Subject Reference", Level.INFO)).isTrue();
    assertEquals(testAppender.getLoggedEvents().get(2).getLevel(), Level.INFO);
    assertThat(userAllocationRole).isNotNull();
    assertThat(userAllocationRole).isNotNull().extracting("role").isEqualTo("Manager");

    // Then someone else may not take manage away from allocation owner by deleting
    tryToDemoteExpectingExceptions(otherUser, rootAllocationOwner, rootAllocation);

    // otherUser can own an allocation, too
    Amount amt = new Amount(Currency.USD, new BigDecimal(0));
    Allocation secondAllocation =
        allocationService
            .createAllocation(
                rootAllocation.getBusinessId(),
                rootAllocation.getId(),
                "Second Allocation",
                otherUser,
                amt,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet())
            .allocation();

    // otherUser gets Manage permission by default
    setCurrentUser(otherUser);
    assertEquals(
        ALLOCATION_MANAGER,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(secondAllocation.getId())
            .allocationRole());

    // rootAllocationOwner inherits owner permissions
    setCurrentUser(rootAllocationOwner);
    assertEquals(
        ALLOCATION_ADMIN,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(secondAllocation.getId())
            .allocationRole());

    // Owner can't go below manager permission
    tryToDemoteExpectingExceptions(rootAllocationOwner, otherUser, secondAllocation);

    // Root Allocation owner makes a thirdAllocation under secondAllocation
    setCurrentUser(rootAllocationOwner);
    Allocation thirdAllocation =
        allocationService
            .createAllocation(
                rootAllocation.getBusinessId(),
                secondAllocation.getId(),
                "Third Allocation",
                rootAllocationOwner,
                amt,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet())
            .allocation();

    // otherUser inherits Manager permission by default
    setCurrentUser(otherUser);
    assertEquals(
        ALLOCATION_MANAGER,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(secondAllocation.getId())
            .allocationRole());
    setCurrentUser(rootAllocationOwner);
    assertEquals(
        ALLOCATION_ADMIN,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(secondAllocation.getId())
            .allocationRole());

    // otherUser's Manager permission cannot be taken away
    tryToDemoteExpectingExceptions(rootAllocationOwner, otherUser, thirdAllocation);

    // otherUser automatically gets Manager by virtue of being an allocation owner
    setCurrentUser(otherUser);
    assertEquals(
        ALLOCATION_MANAGER,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(secondAllocation.getId())
            .allocationRole());

    // Make OtherUser an admin at root
    setCurrentUser(rootAllocationOwner);
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser, rootAllocation, DefaultRoles.ALLOCATION_ADMIN);

    // Admin permission propagates down (lambda because it happens again in a moment)
    Consumer<String> assertAllocationOwnerRole =
        role ->
            Stream.of(secondAllocation, thirdAllocation)
                .forEach(
                    a -> {
                      setCurrentUser(otherUser);
                      assertEquals(
                          role,
                          rolesAndPermissionsService
                              .getUserRolesAndPermissionsForAllocation(a.getId())
                              .allocationRole(),
                          a.getName());
                      tryToDemoteExpectingExceptions(rootAllocationOwner, otherUser, a);
                    });
    assertAllocationOwnerRole.accept(ALLOCATION_ADMIN);

    // Switch otherUser Owner permission back to Manager, and it goes everywhere
    setCurrentUser(rootAllocationOwner);
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser, rootAllocation, ALLOCATION_MANAGER);

    assertAllocationOwnerRole.accept(ALLOCATION_MANAGER);

    // Switch back to admin for secondUser
    setCurrentUser(rootAllocationOwner);
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser, rootAllocation, DefaultRoles.ALLOCATION_ADMIN);

    // Root Allocation owner makes a fourthAllocation under thirdAllocation
    setCurrentUser(rootAllocationOwner);
    Allocation fourthAllocation =
        allocationService
            .createAllocation(
                rootAllocation.getBusinessId(),
                thirdAllocation.getId(),
                "Third Allocation",
                rootAllocationOwner,
                amt,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet())
            .allocation();

    setCurrentUser(otherUser);
    assertEquals(
        ALLOCATION_ADMIN,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(fourthAllocation.getId())
            .allocationRole());
  }

  @Test
  void createOrUpdateUserAllocationRole_CannotUpgradeToHigherThanCurrentUserRole() {
    final User manager1 =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_MANAGER)
            .user();
    final User manager2 =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_MANAGER)
            .user();
    testHelper.setCurrentUser(manager1);

    // Cannot upgrade your own permissions to be higher
    assertThrows(
        InvalidRequestException.class,
        () ->
            rolesAndPermissionsService.createOrUpdateUserAllocationRole(
                manager1,
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_ADMIN));

    // Cannot upgrade someone else to be higher than you are
    assertThrows(
        InvalidRequestException.class,
        () ->
            rolesAndPermissionsService.createOrUpdateUserAllocationRole(
                manager2,
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_ADMIN));
  }

  @Test
  void createAndThenUpdatePermissions() {
    final User newUser = testHelper.createUser(createBusinessRecord.business()).user();
    final Optional<UserAllocationRole> noRoleResult =
        userAllocationRoleRepository.findByUserIdAndAllocationId(
            newUser.getId(), createBusinessRecord.allocationRecord().allocation().getId());
    assertTrue(noRoleResult.isEmpty());

    // Cannot update before create
    assertThrows(
        InvalidRequestException.class,
        () ->
            rolesAndPermissionsService.updateUserAllocationRole(
                newUser,
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE));

    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.createUserAllocationRole(
                newUser,
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE));
    final Optional<UserAllocationRole> employeeRoleResult =
        userAllocationRoleRepository.findByUserIdAndAllocationId(
            newUser.getId(), createBusinessRecord.allocationRecord().allocation().getId());
    assertThat(employeeRoleResult)
        .isPresent()
        .get()
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("userId", newUser.getId())
        .hasFieldOrPropertyWithValue("role", DefaultRoles.ALLOCATION_EMPLOYEE);

    // Cannot run create again
    assertThrows(
        InvalidRequestException.class,
        () ->
            rolesAndPermissionsService.createUserAllocationRole(
                newUser,
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_MANAGER));

    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.updateUserAllocationRole(
                newUser,
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_MANAGER));

    final Optional<UserAllocationRole> managerRoleResult =
        userAllocationRoleRepository.findByUserIdAndAllocationId(
            newUser.getId(), createBusinessRecord.allocationRecord().allocation().getId());
    assertThat(managerRoleResult)
        .isPresent()
        .get()
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("userId", newUser.getId())
        .hasFieldOrPropertyWithValue("role", DefaultRoles.ALLOCATION_MANAGER);
  }

  private void tryToDemoteExpectingExceptions(User actor, User grantee, Allocation allocation) {
    setCurrentUser(actor);

    Supplier<String> msg =
        () -> String.valueOf(Map.of("actor", actor, "grantee", grantee, "allocation", allocation));
    assertThrows(
        Exception.class,
        () ->
            rolesAndPermissionsService.deleteUserAllocationRole(
                allocation.getId(), grantee.getId()),
        msg);

    // ... or by lowering
    assertThrows(
        InvalidRequestException.class,
        () ->
            rolesAndPermissionsService.createOrUpdateUserAllocationRole(
                grantee, allocation, ALLOCATION_VIEW_ONLY),
        msg);
  }

  //      // test if a user has permission on a parent allocation rather than the given allocation
  //      @Test
  //      void testInheritingPermissionFromParent() {
  //        CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());
  //        User allocationOwner = userService.retrieveUser(rootAllocation.getOwner().getId());
  //        User testUser = newUser.user();
  //
  //        // The allocation owner gives someone else manage permission
  //        roleService.grantAllocationPermission(
  //            allocationOwner, testUser, rootAllocation, AllocationPermission.MANAGE);
  //        assertUserHasPermission(testUser, rootAllocation, AllocationPermission.MANAGE);
  //
  //        // Allocation owner makes a child allocation
  //        Amount amt = new Amount(Currency.USD, new BigDecimal(100));
  //        Allocation childAllocation =
  //            allocationService
  //                .createAllocation(
  //                    rootAllocation.getBusinessId(),
  //                    rootAllocation.getId(),
  //                    "Child Allocation",
  //                    allocationOwner,
  //                    amt,
  //                    Collections.emptyMap(),
  //                    Collections.emptyList(),
  //                    Collections.emptySet())
  //                .allocation();
  //
  //        // Now see that the user has manage permission
  //        assertUserHasPermission(testUser, childAllocation, AllocationPermission.MANAGE);
  //      }
  //
  //      // test if a user has permission on a grandparent allocation
  //      @Test
  //      void testAncestorPermission() {
  //        // When the user doesn't have permission on the given allocation, it should
  //        // fall back to the permission of the first ancestor allocation for which the user has
  //        // permission.
  //        CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());
  //        User allocationOwner = userService.retrieveUser(rootAllocation.getOwner().getId());
  //        User testUser = newUser.user();
  //
  //        // Set up a chain of 4 child allocations
  //        List<Allocation> childAllocations = new ArrayList<>();
  //        BigDecimal childAllocationAmount = new BigDecimal(512);
  //        Allocation parent = rootAllocation;
  //        for (int i = 0; i < 6; i++) {
  //          Amount amt = new Amount(Currency.USD, childAllocationAmount);
  //          Allocation childAllocation =
  //              allocationService
  //                  .createAllocation(
  //                      rootAllocation.getBusinessId(),
  //                      parent.getId(),
  //                      "Child " + i,
  //                      allocationOwner,
  //                      amt,
  //                      Collections.emptyMap(),
  //                      Collections.emptyList(),
  //                      Collections.emptySet())
  //                  .allocation();
  //          childAllocations.add(childAllocation);
  //          childAllocationAmount = childAllocationAmount.divide(TWO);
  //          parent = childAllocation;
  //        }
  //
  //        // * Roles will be
  //        // * Root    READ
  //        // * Child 0 READ
  //        // * Child 1 CODE
  //        // * Child 2 CODE
  //        // * Child 3 NONE Like a gift card, for example
  //        // * Child 3 NONE
  //        // * Child 3 NONE
  //
  //        // Assign expected globalUserPermissions
  //        roleService.grantAllocationPermission(
  //            allocationOwner, testUser, rootAllocation, AllocationPermission.READ);
  //        roleService.grantAllocationPermission(
  //            allocationOwner, testUser, childAllocations.get(1), AllocationPermission.CODE);
  //        roleService.grantAllocationPermission(
  //            allocationOwner, testUser, childAllocations.get(3), AllocationPermission.NONE);
  //
  //        // verify permissions
  //        assertUserHasPermission(testUser, rootAllocation, AllocationPermission.READ);
  //        assertUserHasPermission(testUser, childAllocations.get(0), AllocationPermission.READ);
  //        assertUserHasPermission(testUser, childAllocations.get(1), AllocationPermission.CODE);
  //        assertUserHasPermission(testUser, childAllocations.get(2), AllocationPermission.CODE);
  //        assertUserHasPermission(testUser, childAllocations.get(3), AllocationPermission.NONE);
  //        assertUserHasPermission(testUser, childAllocations.get(4), AllocationPermission.NONE);
  //        assertUserHasPermission(testUser, childAllocations.get(5), AllocationPermission.NONE);
  //
  //        // Archived user has no permissions
  //        testUser.setArchived(true);
  //        assertUserLacksPermission(testUser, rootAllocation,
  // AllocationPermission.MANAGE_PERMISSIONS);
  //        for (Allocation allocation : childAllocations) {
  //          assertUserLacksPermission(testUser, allocation,
  // AllocationPermission.MANAGE_PERMISSIONS);
  //        }
  //
  //        // archived Grantee results in InvalidRequestException
  //        assertThrows(
  //            InvalidRequestException.class,
  //            () ->
  //                roleService.grantAllocationPermission(
  //                    allocationOwner, testUser, childAllocations.get(3),
  // AllocationPermission.MANAGE));
  //      }

  @SwitchesCurrentUser(reviewer = "jscarbor", explanation = "for testing")
  @FusionAuthRoleAdministrator(reviewer = "jscarbor", explanation = "For testing")
  @Test
  void testNoMixingAndMatchingBusinessesExceptBookkeepers() {
    // Bootstrap an admin user for this test
    User admin = userService.retrieveUser(rootAllocation.getOwnerId());
    // go under the radar using test privilege to call fusionAuthService for bootstrapping CSM role
    fusionAuthService.changeUserRole(
        RoleChange.GRANT, admin.getSubjectRef(), GLOBAL_CUSTOMER_SERVICE_MANAGER);

    setCurrentUser(admin);

    // Second business to cross boundaries
    CreateBusinessRecord business2 = testHelper.createBusiness();
    Allocation rootAllocation2 = business2.allocationRecord().allocation();
    final User bookkeeper = entityManager.getReference(User.class, rootAllocation2.getOwnerId());

    // Bookkeeper doesn't have the bookkeeper role yet, so should not get read permission
    setCurrentUser(admin);
    assertThrows(
        InvalidRequestException.class,
        () ->
            rolesAndPermissionsService.createOrUpdateUserAllocationRole(
                bookkeeper, rootAllocation, ALLOCATION_VIEW_ONLY));

    // Now grant the role and permit crossing businesses
    setCurrentUser(admin);
    assertTrue(rolesAndPermissionsService.grantGlobalRole(bookkeeper.getId(), GLOBAL_BOOKKEEPER));
    // Granting the second time doesn't actually make a change
    assertFalse(rolesAndPermissionsService.grantGlobalRole(bookkeeper.getId(), GLOBAL_BOOKKEEPER));

    assertTrue(rolesAndPermissionsService.getGlobalRoles(bookkeeper).contains(GLOBAL_BOOKKEEPER));

    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.createOrUpdateUserAllocationRole(
                bookkeeper, rootAllocation, ALLOCATION_VIEW_ONLY));
    entityManager.flush();
    Runnable makeBookkeeperBusinessUser =
        () -> {
          entityManager.flush();
          CurrentUserSwitcher.setCurrentUser(
              new CurrentUser(
                  bookkeeper.getType(),
                  bookkeeper.getId(),
                  rootAllocation.getBusinessId(),
                  rolesAndPermissionsService.getGlobalRoles(bookkeeper)));
        };
    makeBookkeeperBusinessUser.run();

    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ),
                EnumSet.noneOf(GlobalUserPermission.class)));

    // Revoking bookkeeper permission effectively revokes the cross-business permissions
    setCurrentUser(admin);
    rolesAndPermissionsService.revokeGlobalRole(bookkeeper.getId(), GLOBAL_BOOKKEEPER);
    entityManager.flush();
    makeBookkeeperBusinessUser.run();
    assertThrows(
        AccessDeniedException.class,
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ),
                EnumSet.noneOf(GlobalUserPermission.class)));

    // Re-grant the role and see that it works again
    setCurrentUser(admin);
    rolesAndPermissionsService.grantGlobalRole(bookkeeper.getId(), GLOBAL_BOOKKEEPER);
    entityManager.flush();
    makeBookkeeperBusinessUser.run();
    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ),
                EnumSet.noneOf(GlobalUserPermission.class)));

    // archive the user and see that they can do nothing
    setCurrentUser(admin);
    bookkeeper.setArchived(true);
    entityManager.flush();
    makeBookkeeperBusinessUser.run();
    assertThrows(
        AccessDeniedException.class,
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ),
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getUserRolesForAllocation() {
    CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());
    User allocationOwner = userService.retrieveUser(rootAllocation.getOwnerId());

    // The root allocation owner is a BUSINESS_OWNER so has Admin permission by default
    setCurrentUser(rootAllocationOwner);
    assertEquals(
        ALLOCATION_ADMIN,
        rolesAndPermissionsService
            .getUserRolesAndPermissionsForAllocation(rootAllocation.getId())
            .allocationRole());

    // The allocation owner gives someone else some permissions
    User otherUser = newUser.user();
    setCurrentUser(allocationOwner);
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser, rootAllocation, "Manager");
    entityManager.flush();

    // Check what permissions the user has
    setCurrentUser(otherUser);
    UserRolesAndPermissions queriedPermissions =
        rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(rootAllocation.getId());

    // The next 2 lines will have to be updated if the default manager permissions change in the DB
    EnumSet<AllocationPermission> managerPermissions = EnumSet.allOf(AllocationPermission.class);
    managerPermissions.remove(AllocationPermission.MANAGE_USERS);
    managerPermissions.remove(AllocationPermission.LINK_BANK_ACCOUNTS);

    assertEquals(managerPermissions, queriedPermissions.allocationPermissions());
    assertEquals(
        EnumSet.noneOf(GlobalUserPermission.class), queriedPermissions.globalUserPermissions());

    // Then check all the users of this allocation
    Map<TypedId<UserId>, UserRolesAndPermissions> users =
        rolesAndPermissionsService.getAllRolesAndPermissionsForAllocation(rootAllocation.getId());

    assertEquals(2, users.size());
    assertEquals(
        EnumSet.allOf(AllocationPermission.class),
        users.get(allocationOwner.getId()).allocationPermissions());
    assertFalse(users.get(allocationOwner.getId()).inherited());
    assertEquals(managerPermissions, users.get(otherUser.getId()).allocationPermissions());
    assertFalse(users.get(otherUser.getId()).inherited());
  }

  // TODO test getUsersWithPermissionForAllocation - with child allocation returns parent owners,
  // TODO test get businesses an owner has permission to see
  // TODO test allocation owner always has at least Manager role

  @Test
  public void rootAllocationOwnerHasAdminPermission() {
    setCurrentUser(rootAllocationOwner);
    assertUserRolesAndPermissions(
        DefaultRoles.ALLOCATION_ADMIN,
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        false,
        rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(rootAllocation.getId()));

    // Allocation owner makes a child allocation
    Amount amt = new Amount(Currency.USD, new BigDecimal(100));
    Allocation childAllocation =
        allocationService
            .createAllocation(
                rootAllocation.getBusinessId(),
                rootAllocation.getId(),
                "Child Allocation",
                rootAllocationOwner,
                amt,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet())
            .allocation();

    flush();
    // Also admin on a child allocation
    assertUserRolesAndPermissions(
        ALLOCATION_ADMIN,
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        true,
        userAllocationRoleRepository
            .getUserPermissionAtAllocation(
                childAllocation.getBusinessId(),
                childAllocation.getId(),
                rootAllocationOwner.getId(),
                null)
            .orElseThrow());

    // Also admin on a child allocation created by someone else
    User manager = testHelper.createUser(createBusinessRecord.business()).user();
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        manager, rootAllocation, ALLOCATION_MANAGER);

    // Manager makes a child allocation
    setCurrentUser(manager);
    amt = new Amount(Currency.USD, new BigDecimal(100));
    Allocation childAllocation2 =
        allocationService
            .createAllocation(
                rootAllocation.getBusinessId(),
                rootAllocation.getId(),
                "Child Allocation",
                manager,
                amt,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet())
            .allocation();

    setCurrentUser(rootAllocationOwner);
    assertUserRolesAndPermissions(
        ALLOCATION_ADMIN,
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        true,
        userAllocationRoleRepository
            .getUserPermissionAtAllocation(
                childAllocation2.getBusinessId(),
                childAllocation2.getId(),
                rootAllocationOwner.getId(),
                null)
            .orElseThrow());

    // It's not necessary to name the allocation to get root allocation permission
    assertUserRolesAndPermissions(
        ALLOCATION_ADMIN,
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        false,
        userAllocationRoleRepository
            .getUserPermissionAtBusiness(
                childAllocation2.getBusinessId(), rootAllocationOwner.getId(), null)
            .orElseThrow());

    // Manager also has admin permission because she has that permission at the root
    setCurrentUser(manager);
    assertUserRolesAndPermissions(
        ALLOCATION_ADMIN,
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        false,
        userAllocationRoleRepository
            .getUserPermissionAtBusiness(
                childAllocation2.getBusinessId(), rootAllocationOwner.getId(), null)
            .orElseThrow());

    // A newly-created owner has permission on the record
    BusinessOwnerAndUserRecord otherOwner =
        testHelper.createBusinessOwner(
            createBusinessRecord.business().getId(),
            testHelper.generateEmail(),
            testHelper.generatePassword(),
            false);
    assertUserRolesAndPermissions(
        ALLOCATION_ADMIN,
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        false,
        userAllocationRoleRepository
            .getUserPermissionAtBusiness(
                createBusinessRecord.business().getId(), otherOwner.user().getId(), null)
            .orElseThrow());
  }

  @SneakyThrows
  @Test
  public void testUsersHaveNoPermissionsToAnInactiveBusinessAccount() {
    setCurrentUser(rootAllocationOwner);
    createBusinessRecord.business().setStatus(BusinessStatus.SUSPENDED);

    assertUserRolesAndPermissions(
        null,
        EnumSet.noneOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        false,
        rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(rootAllocation.getId()));
  }

  @SneakyThrows
  @Test
  public void testBusinessOwnerHasOnlyReadPermissionsAfterBusinessClosure() {
    setCurrentUser(rootAllocationOwner);
    createBusinessRecord.business().setStatus(BusinessStatus.CLOSED);

    assertUserRolesAndPermissions(
        DefaultRoles.ALLOCATION_VIEW_ONLY, // null allocation from the
        // RolesAndPermissionsService::ensureNonNullPermissions(...)
        EnumSet.of(AllocationPermission.READ, AllocationPermission.VIEW_OWN),
        EnumSet.noneOf(GlobalUserPermission.class),
        false,
        rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(rootAllocation.getId()));
  }

  @SneakyThrows
  @Test
  void ensureMinimumAllocationPermissions_properlyAddsEmployeePermissions() {
    User noPermissionsUser = testHelper.createUser(createBusinessRecord.business()).user();

    // Ensure that the createUser(...) method does not grant any Permissions
    assertThat(
            rolesAndPermissionsService.getAllRolesAndPermissionsForAllocation(
                rootAllocation.getId()))
        .isNotNull()
        .extracting(i -> i.get(noPermissionsUser.getId()))
        .isNull();

    // Execute the method that will should ensure that we have the 'base' level of permissions
    rolesAndPermissionsService.ensureMinimumAllocationPermissions(
        noPermissionsUser, rootAllocation, DefaultRoles.ALLOCATION_EMPLOYEE);

    // Run the same test as above but we should now have permissions
    assertThat(
            rolesAndPermissionsService.getAllRolesAndPermissionsForAllocation(
                rootAllocation.getId()))
        .isNotNull()
        .extracting(i -> i.get(noPermissionsUser.getId()))
        .isNotNull();
  }

  private void setCurrentUser(User user) {
    flush();
    testHelper.setCurrentUser(user);
  }

  private void flush() {
    entityManager.flush();
  }

  private void assertUserRolesAndPermissions(
      String allocationRole,
      EnumSet<AllocationPermission> allocationPermissions,
      EnumSet<GlobalUserPermission> globalUserPermissions,
      boolean isInherited,
      UserRolesAndPermissions perms) {
    flush();
    assertNotNull(perms);
    assertEquals(allocationRole, perms.allocationRole());
    assertEquals(allocationPermissions, perms.allocationPermissions());
    assertEquals(globalUserPermissions, perms.globalUserPermissions());
    assertEquals(isInherited, perms.inherited());
  }
}
