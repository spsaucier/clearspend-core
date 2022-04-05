package com.clearspend.capital.controller.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.security.UserRolesAndPermissionsRecord;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.RolesAndPermissionsService;
import com.clearspend.capital.service.SecuredRolesAndPermissionsService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/roles-and-permissions")
@RequiredArgsConstructor
public class RolesAndPermissionsController {
  private final SecuredRolesAndPermissionsService securedRolesAndPermissionsService;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final BusinessService businessService;
  private final UserService userService;
  private final AllocationService allocationService;

  @GetMapping("/allocation/{allocationId}")
  UserRolesAndPermissionsRecord getUserAllocationRoles(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {

    return new UserRolesAndPermissionsRecord(
        rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(allocationId));
  }

  @GetMapping("/allPermissions/{businessId}")
  public AllocationsAndPermissionsResponse getAllAllocationsAndPermissions(
      @PathVariable(value = "businessId")
          @Parameter(
              required = true,
              name = "businessId",
              description = "ID of the business to get all permissions for.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          final TypedId<BusinessId> businessId) {
    final TypedId<UserId> userId = CurrentUser.getUserId();
    final List<Allocation> allocations =
        allocationService.getAllocationsForBusiness(businessId).stream()
            .map(AllocationRecord::allocation)
            .toList();
    final List<UserRolesAndPermissionsRecord> permissions =
        securedRolesAndPermissionsService
            .getAllPermissionsForUserInBusiness(userId, businessId)
            .stream()
            .map(UserRolesAndPermissionsRecord::new)
            .toList();
    return new AllocationsAndPermissionsResponse(allocations, permissions);
  }

  @GetMapping("/business/{businessId}")
  UserRolesAndPermissionsRecord getUserAllocationRolesForBusiness(
      @PathVariable(value = "businessId")
          @Parameter(
              required = true,
              name = "businessId",
              description = "ID of the business to check root permissions.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessId> businessId) {
    return new UserRolesAndPermissionsRecord(
        rolesAndPermissionsService.getUserRolesAndPermissionsAtRootAllocation(businessId));
  }

  @GetMapping("/")
  UserRolesAndPermissionsRecord getUserAllocationRolesForOwnBusiness() {
    return getUserAllocationRolesForBusiness(CurrentUser.get().businessId());
  }

  record AllocationsAndPermissionsResponse(
      List<Allocation> allocations, List<UserRolesAndPermissionsRecord> userRoles) {}
}
