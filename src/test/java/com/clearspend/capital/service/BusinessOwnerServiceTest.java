package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedRequest;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BusinessOwnerServiceTest extends BaseCapitalTest {
  private final BusinessOwnerService businessOwnerService;
  private final TestHelper testHelper;
  private final TestDataHelper testDataHelper;
  private final PermissionValidationHelper permissionValidationHelper;
  private CreateBusinessRecord createBusinessRecord;
  private static final Set<String> ROOT_FAILING_ROLES =
      Set.of(
          DefaultRoles.ALLOCATION_EMPLOYEE,
          DefaultRoles.ALLOCATION_VIEW_ONLY,
          DefaultRoles.ALLOCATION_MANAGER);

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
  }

  @Test
  void allOwnersProvided_UserPermissions() {
    final OwnersProvidedRequest request = new OwnersProvidedRequest(false, false);
    final ThrowingRunnable action =
        () ->
            businessOwnerService.allOwnersProvided(
                createBusinessRecord.business().getId(), request);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void validateOwner_UserPermissions() {
    final BusinessOwnerData businessOwnerData = new BusinessOwnerData();
    businessOwnerData.setBusinessId(createBusinessRecord.business().getId());
    final ThrowingRunnable action = () -> businessOwnerService.validateOwner(businessOwnerData);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void validateBusinessOwners_UserPermissions() {
    createBusinessRecord.business().setType(BusinessType.INDIVIDUAL);
    final OwnersProvidedRequest request = new OwnersProvidedRequest(false, false);
    final ThrowingRunnable action =
        () ->
            businessOwnerService.validateBusinessOwners(
                createBusinessRecord.business().getId(), request);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void createBusinessOwnerAndStripePerson_UserPermissions() {
    final BusinessOwnerData data =
        testDataHelper.createBusinessOwnerData(createBusinessRecord.business().getId());
    final ThrowingRunnable action =
        () ->
            businessOwnerService.createBusinessOwnerAndStripePerson(
                createBusinessRecord.business().getId(), data);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void updateBusinessOwnerAndStripePerson_WithUser_UserPermissions() {
    final ThrowingRunnable action =
        () -> businessOwnerService.updateBusinessOwnerAndStripePerson(createBusinessRecord.user());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void updateBusinessOwnerAndStripePerson_WithBusinessId_UserPermissions() {
    final BusinessOwnerData data =
        testDataHelper.createBusinessOwnerData(createBusinessRecord.business().getId());
    data.setBusinessOwnerId(createBusinessRecord.businessOwner().getId());
    final ThrowingRunnable action =
        () ->
            businessOwnerService.updateBusinessOwnerAndStripePerson(
                createBusinessRecord.business().getId(), data);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void retrieveBusinessOwnerNotThrowingException_UserPermissions() {
    final ThrowingRunnable action =
        () ->
            businessOwnerService.retrieveBusinessOwnerNotThrowingException(
                createBusinessRecord.businessOwner().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void secureFindBusinessOwner_UserPermissions() {
    final ThrowingRunnable action =
        () -> businessOwnerService.secureFindBusinessOwner(createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }

  @Test
  void deleteBusinessOwner_UserPermissions() {
    final ThrowingRunnable action =
        () ->
            businessOwnerService.deleteBusinessOwner(
                createBusinessRecord.businessOwner().getId(),
                createBusinessRecord.business().getId());
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .useDefaultChildAllocation()
        .addAllChildAllocationFailingRoles(DefaultRoles.ALL_ALLOCATION)
        .addAllRootAllocationFailingRoles(ROOT_FAILING_ROLES)
        .build()
        .validateServiceMethod(action);
  }
}
