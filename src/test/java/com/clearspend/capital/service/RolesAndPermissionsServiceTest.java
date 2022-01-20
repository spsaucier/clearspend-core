package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class RolesAndPermissionsServiceTest extends BaseCapitalTest {

  private static final BigDecimal TWO = new BigDecimal(2);

  @Autowired private TestHelper testHelper;
  @Autowired AllocationService allocationService;
  @Autowired UserAllocationRoleRepository userAllocationRoleRepository;
  @Autowired RolesAndPermissionsService rolesAndPermissionsService;
  @Autowired UserService userService;
  @Autowired BusinessBankAccountService businessBankAccountService;
  @Autowired EntityManager entityManager;

  private CreateBusinessRecord createBusinessRecord;
  private Allocation rootAllocation;
  private User rootAllocationOwner;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.createBusiness();
    rootAllocation = createBusinessRecord.allocationRecord().allocation();
    rootAllocationOwner = entityManager.getReference(User.class, rootAllocation.getOwnerId());

    // Give the root allocation some money
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId()).getId();
    businessBankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("720.51")),
        false);
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

  /*
      // test for an error lowering permission of an allocation owner below manager
      @Test
      void testNoLoweringAllocationOwnerPermission() {
        CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());
        User allocationOwner = userService.retrieveUser(rootAllocation.getOwner().getId());
        User otherUser = newUser.user();

        // The allocation owner gives someone else manage permission
        roleService.grantAllocationPermission(
            allocationOwner, otherUser, rootAllocation, AllocationPermission.MANAGE);
        assertUserHasPermission(otherUser, rootAllocation, AllocationPermission.MANAGE);

        // Then someone else tries to take manage away from allocation owner
        assertThrows(
            ForbiddenException.class,
            () ->
                roleService.grantAllocationPermission(
                    otherUser, allocationOwner, rootAllocation, AllocationPermission.NONE));
      }

      // test if a user has permission on a parent allocation rather than the given allocation
      @Test
      void testInheritingPermissionFromParent() {
        CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());
        User allocationOwner = userService.retrieveUser(rootAllocation.getOwner().getId());
        User testUser = newUser.user();

        // The allocation owner gives someone else manage permission
        roleService.grantAllocationPermission(
            allocationOwner, testUser, rootAllocation, AllocationPermission.MANAGE);
        assertUserHasPermission(testUser, rootAllocation, AllocationPermission.MANAGE);

        // Allocation owner makes a child allocation
        Amount amt = new Amount(Currency.USD, new BigDecimal(100));
        Allocation childAllocation =
            allocationService
                .createAllocation(
                    rootAllocation.getBusinessId(),
                    rootAllocation.getId(),
                    "Child Allocation",
                    allocationOwner,
                    amt,
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    Collections.emptySet())
                .allocation();

        // Now see that the user has manage permission
        assertUserHasPermission(testUser, childAllocation, AllocationPermission.MANAGE);
      }

      // test if a user has permission on a grandparent allocation
      @Test
      void testAncestorPermission() {
        // When the user doesn't have permission on the given allocation, it should
        // fall back to the permission of the first ancestor allocation for which the user has
        // permission.
        CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());
        User allocationOwner = userService.retrieveUser(rootAllocation.getOwner().getId());
        User testUser = newUser.user();

        // Set up a chain of 4 child allocations
        List<Allocation> childAllocations = new ArrayList<>();
        BigDecimal childAllocationAmount = new BigDecimal(512);
        Allocation parent = rootAllocation;
        for (int i = 0; i < 6; i++) {
          Amount amt = new Amount(Currency.USD, childAllocationAmount);
          Allocation childAllocation =
              allocationService
                  .createAllocation(
                      rootAllocation.getBusinessId(),
                      parent.getId(),
                      "Child " + i,
                      allocationOwner,
                      amt,
                      Collections.emptyMap(),
                      Collections.emptyList(),
                      Collections.emptySet())
                  .allocation();
          childAllocations.add(childAllocation);
          childAllocationAmount = childAllocationAmount.divide(TWO);
          parent = childAllocation;
        }

        // * Roles will be
        // * Root    READ
        // * Child 0 READ
        // * Child 1 CODE
        // * Child 2 CODE
        // * Child 3 NONE Like a gift card, for example
        // * Child 3 NONE
        // * Child 3 NONE

        // Assign expected globalUserPermissions
        roleService.grantAllocationPermission(
            allocationOwner, testUser, rootAllocation, AllocationPermission.READ);
        roleService.grantAllocationPermission(
            allocationOwner, testUser, childAllocations.get(1), AllocationPermission.CODE);
        roleService.grantAllocationPermission(
            allocationOwner, testUser, childAllocations.get(3), AllocationPermission.NONE);

        // verify permissions
        assertUserHasPermission(testUser, rootAllocation, AllocationPermission.READ);
        assertUserHasPermission(testUser, childAllocations.get(0), AllocationPermission.READ);
        assertUserHasPermission(testUser, childAllocations.get(1), AllocationPermission.CODE);
        assertUserHasPermission(testUser, childAllocations.get(2), AllocationPermission.CODE);
        assertUserHasPermission(testUser, childAllocations.get(3), AllocationPermission.NONE);
        assertUserHasPermission(testUser, childAllocations.get(4), AllocationPermission.NONE);
        assertUserHasPermission(testUser, childAllocations.get(5), AllocationPermission.NONE);

        // Archived user has no permissions
        testUser.setArchived(true);
        assertUserLacksPermission(testUser, rootAllocation, AllocationPermission.MANAGE_PERMISSIONS);
        for (Allocation allocation : childAllocations) {
          assertUserLacksPermission(testUser, allocation, AllocationPermission.MANAGE_PERMISSIONS);
        }

        // archived Grantee results in InvalidRequestException
        assertThrows(
            InvalidRequestException.class,
            () ->
                roleService.grantAllocationPermission(
                    allocationOwner, testUser, childAllocations.get(3), AllocationPermission.MANAGE));
      }
  */

  /*
  @Test
  void testNoMixingAndMatchingBusinessesExceptBookkeepers() {
    // Bootstrap an admin user for this test
    User admin = userService.retrieveUser(rootAllocation.getOwner().getId());
    rolesAndPermissionsService.grantPermission(admin,
        GlobalUserPermission.CUSTOMER_SERVICE_MANAGER);

    // Second business to cross boundaries
    CreateBusinessRecord business2 = testHelper.createBusiness();
    Allocation rootAllocation2 = business2.allocationRecord().allocation();
    final User bookkeeper = rootAllocation2.getOwner();

    // Bookkeeper doesn't have the bookkeeper role yet, so should not get read permission
    entityManager.flush();
    assertThrows(
        ForbiddenException.class,
        () ->
            rolesAndPermissionsService.createUpdateAllocationUserRole(
                admin, bookkeeper, rootAllocation, EnumSet.of(AllocationPermission.READ)));

    // Now grant the role and permit crossing businesses
    rolesAndPermissionsService.grantPermission(
        admin, bookkeeper, GlobalUserPermission.CROSS_BUSINESS_BOUNDARY);
    entityManager.flush();
    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.createUpdateAllocationUserRole(
                admin, bookkeeper, rootAllocation, EnumSet.of(AllocationPermission.READ)));
    entityManager.flush();
    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getBusinessId(),
                bookkeeper.getId(),
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ)));

    // Revoking bookkeeper permission effectively revokes the cross-business permissions
    rolesAndPermissionsService.revokeRole(admin, bookkeeper,
        GlobalUserPermission.CROSS_BUSINESS_BOUNDARY);
    entityManager.flush();
    assertThrows(
        ForbiddenException.class,
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getBusinessId(),
                bookkeeper.getId(),
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ)));

    // Re-grant the role and see that it works again
    rolesAndPermissionsService.grantPermission(
        admin, bookkeeper, GlobalUserPermission.CROSS_BUSINESS_BOUNDARY);
    entityManager.flush();
    assertDoesNotThrow(
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getBusinessId(),
                bookkeeper.getId(),
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ)));

    // archive the user and see that they can do nothing
    bookkeeper.setArchived(true);
    entityManager.flush();
    assertThrows(
        ForbiddenException.class,
        () ->
            rolesAndPermissionsService.assertUserHasPermission(
                rootAllocation.getBusinessId(),
                bookkeeper.getId(),
                rootAllocation.getId(),
                EnumSet.of(AllocationPermission.READ)));
  }

   */

  /*
    @Test
    void getUserRolesForAllocation() {
      CreateUpdateUserRecord newUser = testHelper.createUser(createBusinessRecord.business());
      User allocationOwner = userService.retrieveUser(rootAllocation.getOwner().getId());
      User otherUser = newUser.user();

      // The allocation owner gives someone else some permissions
      rolesAndPermissionsService.createUpdateAllocationUserRole(
          allocationOwner, otherUser, rootAllocation, "Owner");
      entityManager.flush();
      assertUserHasPermission(otherUser, rootAllocation, AllocationPermission.ALLOCATION_OWNER_ROLE);
      // but not all permissions
      assertUserLacksPermission(
          otherUser,
          rootAllocation,
          EnumSet.complementOf(AllocationPermission.ALLOCATION_OWNER_ROLE));

      Map<TypedId<UserId>, PermissionDTO> users =
          rolesAndPermissionsService.getUsersWithPermissionForAllocation(
              allocationOwner.getId(), rootAllocation.getBusinessId(), rootAllocation.getId());
      assertEquals(2, users.size());
      assertEquals(
          AllocationPermission.ROOT_ALLOCATION_OWNER_ROLE,
          users.get(allocationOwner.getId()).allocationPermissions());
      assertFalse(users.get(allocationOwner.getId()).inherited());
      assertEquals(
          AllocationPermission.ALLOCATION_OWNER_ROLE, users.get(otherUser.getId()).allocationPermissions());
      assertFalse(users.get(otherUser.getId()).inherited());
    }
  */
  // TODO test getUsersWithPermissionForAllocation - with child allocation returns parent owners,
  // TODO test get businesses an owner has permission to see
  // TODO test allocation owner always has at least Manager role

  @Test
  public void rootAllocationOwnerHasAdminPermission() {
    setCurrentUser(rootAllocationOwner);
    assertUserRolesAndPermissions(
        "Admin",
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
                Collections.emptyList(),
                Collections.emptySet())
            .allocation();

    flush();
    // Also admin on a child allocation
    assertUserRolesAndPermissions(
        "Admin",
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        true,
        userAllocationRoleRepository.getUserPermissionAtAllocation(
            childAllocation.getBusinessId(),
            childAllocation.getId(),
            rootAllocationOwner.getId(),
            null));

    // Also admin on a child allocation created by someone else
    User manager = testHelper.createUser(createBusinessRecord.business()).user();
    rolesAndPermissionsService.setUserAllocationRole(
        rootAllocation.getId(), manager.getId(), "Manager");

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
                Collections.emptyList(),
                Collections.emptySet())
            .allocation();

    setCurrentUser(rootAllocationOwner);
    assertUserRolesAndPermissions(
        "Admin",
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        true,
        userAllocationRoleRepository.getUserPermissionAtAllocation(
            childAllocation2.getBusinessId(),
            childAllocation2.getId(),
            rootAllocationOwner.getId(),
            null));

    // It's not necessary to name the allocation to get root allocation permission
    assertUserRolesAndPermissions(
        "Admin",
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        true,
        userAllocationRoleRepository.getUserPermissionAtBusiness(
            childAllocation2.getBusinessId(), rootAllocationOwner.getId(), null));

    // Manager also has admin permission because she has that permission at the root
    setCurrentUser(manager);
    assertUserRolesAndPermissions(
        "Admin",
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.noneOf(GlobalUserPermission.class),
        true,
        userAllocationRoleRepository.getUserPermissionAtBusiness(
            childAllocation2.getBusinessId(), rootAllocationOwner.getId(), null));
  }

  private void setCurrentUser(User user) {
    flush();
    CurrentUserSwitcher.setCurrentUser(user);
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
