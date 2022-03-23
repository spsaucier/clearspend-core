package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.type.business.UpdateBusiness;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.util.Set;
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
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
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
        .addAllRootAllocationFailingRoles(
            Set.of(
                DefaultRoles.ALLOCATION_EMPLOYEE,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void getBusiness_UserPermissions() {
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    final ThrowingRunnable action =
        () -> businessService.getBusiness(createBusinessRecord.business().getId(), true);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
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
                allocation1.getId(),
                allocation2.getParentAllocationId(),
                amount);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .addAllRootAllocationFailingRoles(
            Set.of(DefaultRoles.ALLOCATION_EMPLOYEE, DefaultRoles.ALLOCATION_VIEW_ONLY))
        .build()
        .validateServiceMethod(action);
  }
}
