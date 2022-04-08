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
import java.util.function.Function;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    final ThrowingRunnable action =
        () -> allocationService.getAllocationsForBusiness(createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .build()
        .validateServiceMethod(action);

    final Function<List<AllocationRecord>, List<Allocation>> toAllocations =
        records -> records.stream().map(AllocationRecord::allocation).toList();

    testHelper.setCurrentUser(manager);
    final List<AllocationRecord> managerAllocations =
        allocationService.getAllocationsForBusiness(createBusinessRecord.business().getId());
    assertThat(toAllocations.apply(managerAllocations)).hasSize(3);

    // Employee should only see self-owned allocation
    testHelper.setCurrentUser(employee);
    final List<AllocationRecord> employeeAllocations =
        allocationService.getAllocationsForBusiness(createBusinessRecord.business().getId());
    assertThat(toAllocations.apply(employeeAllocations)).hasSize(1);
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
    rolesAndPermissionsService.createUserAllocationRole(
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
