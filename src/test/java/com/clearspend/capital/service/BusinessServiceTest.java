package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.type.business.UpdateBusiness;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BusinessServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;

  @Autowired private BusinessRepository businessRepository;
  @Autowired private PermissionValidationHelper permissionValidationHelper;
  @Autowired private BusinessService businessService;

  @Test
  void createBusiness() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business foundBusiness =
        businessRepository.findById(createBusinessRecord.business().getId()).orElseThrow();
    assertThat(foundBusiness).isNotNull();
  }

  @Test
  void updateBusinessAccountingSetupStep_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final ThrowingRunnable action =
        () ->
            businessService.updateBusinessAccountingSetupStep(
                createBusinessRecord.business().getId(), AccountingSetupStep.ADD_CREDIT_CARD);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void updateBusiness_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final UpdateBusiness updateBusiness = new UpdateBusiness();
    updateBusiness.setBusinessName("FooBar");
    final ThrowingRunnable action =
        () ->
            businessService.updateBusiness(createBusinessRecord.business().getId(), updateBusiness);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getBusiness_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();

    testHelper.setCurrentUser(otherBusiness.user());
    final User noRole = testHelper.createUser(otherBusiness.business()).user();

    final ThrowingRunnable action =
        () -> businessService.getBusiness(otherBusiness.business().getId(), true);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        // Testing that a user, any user, within the business can use the method
        .allowUser(noRole)
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER,
                DefaultRoles.GLOBAL_VIEWER))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void reallocateBusinessFunds_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Allocation allocation1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Allocation1",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation allocation2 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Allocation2",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    final Amount amount = Amount.of(Currency.USD);

    final ThrowingRunnable action =
        () ->
            businessService.reallocateBusinessFunds(
                createBusinessRecord.business().getId(),
                createBusinessRecord.user().getId(),
                allocation1.getId(),
                allocation2.getParentAllocationId(),
                amount);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .build()
        .validateServiceMethod(action);
  }

  @SneakyThrows
  @Test
  void suspendBusiness_requiresCustomerServicePermission() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final CreateBusinessRecord globalUserRecord = testHelper.createBusiness();

    final ThrowingRunnable action =
        () ->
            businessService.updateBusinessStatus(
                createBusinessRecord.user().getBusinessId(), BusinessStatus.SUSPENDED);

    permissionValidationHelper
        .buildValidator(globalUserRecord)
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER, DefaultRoles.GLOBAL_CUSTOMER_SERVICE))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  public void enableAutomaticExpenseCategories_isFalseByDefault() {
    assertThat(testHelper.createBusiness().business().getAutoCreateExpenseCategories()).isFalse();
  }

  @Test
  public void setAutomaticExpenseCategories_persistsAppropriately() {
    CreateBusinessRecord record = testHelper.createBusiness();
    Business business = record.business();
    testHelper.setCurrentUser(record.user());
    assertThat(business.getAutoCreateExpenseCategories()).isFalse();

    businessService.setAutomaticExpenseCategories(business.getBusinessId(), true);
    assertThat(business.getAutoCreateExpenseCategories()).isTrue();
    // Updating to the current value works
    businessService.setAutomaticExpenseCategories(business.getBusinessId(), true);
    assertThat(business.getAutoCreateExpenseCategories()).isTrue();

    businessService.setAutomaticExpenseCategories(business.getBusinessId(), false);
    assertThat(business.getAutoCreateExpenseCategories()).isFalse();
  }

  @Test
  public void setAutomaticExpenseCategories_permissionsCheck() {
    CreateBusinessRecord record = testHelper.createBusiness();
    testHelper.setCurrentUser(record.user());
    Allocation childAllocation =
        testHelper
            .createAllocation(
                record.business().getBusinessId(),
                "Sub-Allocation",
                record.allocationRecord().allocation().getId(),
                record.user())
            .allocation();

    final ThrowingRunnable action =
        () ->
            businessService.setAutomaticExpenseCategories(record.business().getBusinessId(), true);

    permissionValidationHelper
        .buildValidator(record)
        .setAllocation(childAllocation)
        .allowRolesOnRootAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .build()
        .validateServiceMethod(action);
  }
}
