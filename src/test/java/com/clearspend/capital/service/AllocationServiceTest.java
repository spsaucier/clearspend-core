package com.clearspend.capital.service;

import static com.clearspend.capital.data.model.security.DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.AssertionHelper;
import com.clearspend.capital.AssertionHelper.PostingAssertion;
import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.FusionAuthService.FusionAuthRoleAdministrator;
import com.clearspend.capital.service.FusionAuthService.RoleChange;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
public class AllocationServiceTest extends BaseCapitalTest {

  @Autowired private AssertionHelper assertionHelper;
  @Autowired private TestHelper testHelper;

  @Autowired private AllocationService allocationService;
  @Autowired private LedgerService ledgerService;
  @Autowired private FusionAuthService fusionAuthService;
  @Autowired private RolesAndPermissionsService rolesAndPermissionsService;
  @Autowired private UserService userService;
  @Autowired private PermissionValidationHelper permissionValidationHelper;

  @Autowired private EntityManager entityManager;

  private CreateBusinessRecord createBusinessRecord;
  private Allocation rootAllocation;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.createBusiness();
    rootAllocation = createBusinessRecord.allocationRecord().allocation();
    testHelper.setCurrentUser(createBusinessRecord.user());
  }

  @Test
  void getAllocationsForBusiness() {
    final AllocationRecord child =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "Child",
            rootAllocation.getId(),
            createBusinessRecord.user());

    final List<AllocationRecord> result =
        allocationService.getAllocationsForBusiness(createBusinessRecord.business().getId());
    assertThat(result).hasSize(2).contains(createBusinessRecord.allocationRecord(), child);
  }

  @Test
  void getAllocationsForBusiness_UserPermissions() {
    final User manager =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_MANAGER)
            .user();
    final User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();
    final Allocation childAllocation1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child1",
                rootAllocation.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation childAllocation2 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(), "Child2", rootAllocation.getId(), employee)
            .allocation();
    final User viewOnlyChild1 =
        testHelper.createUserWithRole(childAllocation1, DefaultRoles.ALLOCATION_VIEW_ONLY).user();

    final ThrowingSupplier<List<AllocationRecord>> action =
        () -> allocationService.getAllocationsForBusiness(createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        // Employee users should not be able to see any allocations
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_EMPLOYEE, records -> assertThat(records).isEmpty())
        // PermissionsValidationHelper default behavior is to assign roles to a user at the root
        // allocation, so these should all see all allocations
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY),
            records ->
                assertThat(toAllocations(records))
                    .hasSize(3)
                    .contains(rootAllocation, childAllocation1, childAllocation2))
        // This user should only be able to see allocations they are a member of
        .<List<AllocationRecord>>allowUserWithResult(
            viewOnlyChild1,
            records -> assertThat(toAllocations(records)).hasSize(1).contains(childAllocation1))
        // These global roles have access to everything
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER,
                DefaultRoles.GLOBAL_VIEWER),
            records ->
                assertThat(toAllocations(records))
                    .hasSize(3)
                    .contains(rootAllocation, childAllocation1, childAllocation2))
        // These roles should not see any results
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(DefaultRoles.GLOBAL_BOOKKEEPER, DefaultRoles.GLOBAL_RESELLER),
            records -> assertThat(records).isEmpty())
        .build()
        .validateServiceMethod(action);
  }

  private List<Allocation> toAllocations(final List<AllocationRecord> allocationRecords) {
    return allocationRecords.stream().map(AllocationRecord::allocation).toList();
  }

  @Test
  void getSingleAllocation_UserPermissions() {
    final ThrowingRunnable action =
        () ->
            allocationService.getSingleAllocation(
                createBusinessRecord.business().getId(),
                createBusinessRecord.allocationRecord().allocation().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_VIEWER,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getAllocation_UserPermissions() {
    final ThrowingRunnable action =
        () ->
            allocationService.getAllocation(
                createBusinessRecord.business(),
                createBusinessRecord.allocationRecord().allocation().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_VIEWER,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getAllocationChildren_UserPermissions() {
    final Allocation root = createBusinessRecord.allocationRecord().allocation();
    final Allocation child1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child1",
                root.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation child2 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child2",
                root.getId(),
                createBusinessRecord.user())
            .allocation();
    final User viewOnlyChild2 =
        testHelper.createUserWithRole(child2, DefaultRoles.ALLOCATION_VIEW_ONLY).user();
    final ThrowingSupplier<List<AllocationRecord>> action =
        () ->
            allocationService.getAllocationChildren(
                createBusinessRecord.business(),
                createBusinessRecord.allocationRecord().allocation().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        // These roles are set on the root allocation by the validator, and should see everything
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY),
            records -> assertThat(toAllocations(records)).hasSize(2).contains(child1, child2))
        // Employees cant see any allocations
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_EMPLOYEE, records -> assertThat(records).isEmpty())
        // These global roles should see everything
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_VIEWER,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                GLOBAL_CUSTOMER_SERVICE_MANAGER),
            records -> assertThat(toAllocations(records)).hasSize(2).contains(child1, child2))
        // These global roles should see nothing
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(DefaultRoles.GLOBAL_BOOKKEEPER, DefaultRoles.GLOBAL_RESELLER),
            records -> assertThat(toAllocations(records)).isEmpty())
        // This user should only see the child allocation they have the role on
        .<List<AllocationRecord>>allowUserWithResult(
            viewOnlyChild2,
            records -> assertThat(toAllocations(records)).hasSize(1).contains(child2))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void searchBusinessAllocations_NoName_UserPermissions() {
    final Allocation root = createBusinessRecord.allocationRecord().allocation();
    final Allocation child1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child1",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final User viewOnlyChild1 =
        testHelper.createUserWithRole(child1, DefaultRoles.ALLOCATION_VIEW_ONLY).user();
    final ThrowingSupplier<List<AllocationRecord>> action =
        () -> allocationService.searchBusinessAllocations(createBusinessRecord.business());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        // These roles are set on the root allocation by the validator, and should see everything
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY),
            records -> assertThat(toAllocations(records)).hasSize(2).contains(root, child1))
        // Employees can't see any allocations
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_EMPLOYEE, records -> assertThat(records).isEmpty())
        // These global roles should see everything
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_VIEWER,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                GLOBAL_CUSTOMER_SERVICE_MANAGER),
            records -> assertThat(toAllocations(records)).hasSize(2).contains(root, child1))
        // These global roles should see nothing
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(DefaultRoles.GLOBAL_BOOKKEEPER, DefaultRoles.GLOBAL_RESELLER),
            records -> assertThat(toAllocations(records)).isEmpty())
        // This user should only see the child allocation they have the role on
        .<List<AllocationRecord>>allowUserWithResult(
            viewOnlyChild1,
            records -> assertThat(toAllocations(records)).hasSize(1).contains(child1))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void searchBusinessAllocations_UserPermissions() {
    final Allocation root = createBusinessRecord.allocationRecord().allocation();
    final Allocation child1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child1",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final User viewOnlyChild1 =
        testHelper.createUserWithRole(child1, DefaultRoles.ALLOCATION_VIEW_ONLY).user();
    final ThrowingSupplier<List<AllocationRecord>> action =
        () -> allocationService.searchBusinessAllocations(createBusinessRecord.business(), "");
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        // These roles are set on the root allocation by the validator, and should see everything
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY),
            records -> assertThat(toAllocations(records)).hasSize(2).contains(root, child1))
        // Employees can't see any allocations
        .<List<AllocationRecord>>allowRolesOnAllocationWithResult(
            DefaultRoles.ALLOCATION_EMPLOYEE, records -> assertThat(records).isEmpty())
        // These global roles should see everything
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(
                DefaultRoles.GLOBAL_VIEWER,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                GLOBAL_CUSTOMER_SERVICE_MANAGER),
            records -> assertThat(toAllocations(records)).hasSize(2).contains(root, child1))
        // These global roles should see nothing
        .<List<AllocationRecord>>allowGlobalRolesWithResult(
            Set.of(DefaultRoles.GLOBAL_BOOKKEEPER, DefaultRoles.GLOBAL_RESELLER),
            records -> assertThat(toAllocations(records)).isEmpty())
        // This user should only see the child allocation they have the role on
        .<List<AllocationRecord>>allowUserWithResult(
            viewOnlyChild1,
            records -> assertThat(toAllocations(records)).hasSize(1).contains(child1))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void createAllocationCheckPermissions() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    CreateUpdateUserRecord peon = testHelper.createUser(createBusinessRecord.business());
    assertDoesNotThrow(
        () ->
            allocationService.createAllocation(
                createBusinessRecord.business().getId(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                "not root",
                createBusinessRecord.user(),
                new Amount(Currency.USD, new BigDecimal(0)),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet()));

    testHelper.setCurrentUser(createBusinessRecord.user());
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        peon.user(), createBusinessRecord.allocationRecord().allocation(), "View only");
    entityManager.flush();

    testHelper.setCurrentUser(peon.user());
    assertThrows(
        AccessDeniedException.class,
        () ->
            allocationService.createAllocation(
                createBusinessRecord.business().getId(),
                createBusinessRecord.allocationRecord().allocation().getId(),
                "also not root",
                peon.user(),
                new Amount(Currency.USD, new BigDecimal(0)),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptySet()));
  }

  @FusionAuthRoleAdministrator(reviewer = "jscarbor", explanation = "For testing")
  @Test
  void updateAllocationBalance_addRemove_success() {
    // Bootstrap an admin user for this test
    User admin = userService.retrieveUser(rootAllocation.getOwnerId());
    fusionAuthService.changeUserRole(
        RoleChange.GRANT, admin.getSubjectRef(), GLOBAL_CUSTOMER_SERVICE_MANAGER);
    testHelper.setCurrentUser(admin);

    Amount amount = Amount.of(createBusinessRecord.business().getCurrency(), BigDecimal.TEN);
    AdjustmentRecord adjustmentRecord =
        allocationService.updateAllocationBalance(
            createBusinessRecord.business().getId(),
            rootAllocation.getId(),
            amount,
            "Funds added for testing");

    assertionHelper.assertBalance(
        createBusinessRecord.business(),
        rootAllocation,
        adjustmentRecord.account(),
        amount.getAmount(),
        amount.getAmount());
    assertionHelper.assertAdjustment(
        adjustmentRecord.adjustment(), AdjustmentType.MANUAL, adjustmentRecord.account(), amount);
    assertionHelper.assertJournalEntry(
        adjustmentRecord.journalEntry(),
        List.of(
            new PostingAssertion(
                adjustmentRecord.account().getLedgerAccountId(),
                LedgerAccountType.ALLOCATION,
                amount),
            new PostingAssertion(
                ledgerService
                    .getOrCreateLedgerAccount(LedgerAccountType.MANUAL, amount.getCurrency())
                    .getId(),
                LedgerAccountType.MANUAL,
                amount.negate())));
    assertionHelper.assertAdjustmentAccountActivity(
        AccountActivityType.MANUAL,
        adjustmentRecord.adjustment(),
        admin,
        rootAllocation,
        adjustmentRecord.account(),
        amount,
        amount);

    amount = Amount.of(createBusinessRecord.business().getCurrency(), BigDecimal.ONE.negate());
    adjustmentRecord =
        allocationService.updateAllocationBalance(
            createBusinessRecord.business().getId(),
            rootAllocation.getId(),
            amount,
            "Funds removed for testing");

    assertionHelper.assertBalance(
        createBusinessRecord.business(),
        rootAllocation,
        adjustmentRecord.account(),
        BigDecimal.valueOf(9L),
        BigDecimal.valueOf(9L));
    assertionHelper.assertAdjustment(
        adjustmentRecord.adjustment(), AdjustmentType.MANUAL, adjustmentRecord.account(), amount);
    assertionHelper.assertJournalEntry(
        adjustmentRecord.journalEntry(),
        List.of(
            new PostingAssertion(
                adjustmentRecord.account().getLedgerAccountId(),
                LedgerAccountType.ALLOCATION,
                amount),
            new PostingAssertion(
                ledgerService
                    .getOrCreateLedgerAccount(LedgerAccountType.MANUAL, amount.getCurrency())
                    .getId(),
                LedgerAccountType.MANUAL,
                amount.negate())));
    assertionHelper.assertAdjustmentAccountActivity(
        AccountActivityType.MANUAL,
        adjustmentRecord.adjustment(),
        admin,
        rootAllocation,
        adjustmentRecord.account(),
        amount,
        amount);
  }

  @FusionAuthRoleAdministrator(reviewer = "jscarbor", explanation = "For testing")
  @Test
  void updateAllocationBalance_excessiveAddRemove_fail() {
    // Bootstrap an admin user for this test
    User admin = userService.retrieveUser(rootAllocation.getOwnerId());
    fusionAuthService.changeUserRole(
        RoleChange.GRANT, admin.getSubjectRef(), GLOBAL_CUSTOMER_SERVICE_MANAGER);
    testHelper.setCurrentUser(admin);

    Amount amount =
        Amount.of(createBusinessRecord.business().getCurrency(), BigDecimal.valueOf(1000.01));
    assertThrows(
        InvalidRequestException.class,
        () ->
            allocationService.updateAllocationBalance(
                createBusinessRecord.business().getId(),
                rootAllocation.getId(),
                amount,
                "Funds added for testing"));

    assertThrows(
        InvalidRequestException.class,
        () ->
            allocationService.updateAllocationBalance(
                createBusinessRecord.business().getId(),
                rootAllocation.getId(),
                amount.negate(),
                "Funds removed for testing"));
  }
}
