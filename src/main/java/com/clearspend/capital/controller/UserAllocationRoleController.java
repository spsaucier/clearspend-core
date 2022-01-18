package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.userAllocationRole.UserAllocationRoleRecord;
import com.clearspend.capital.controller.type.userAllocationRole.UserAllocationRolesResponse;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.RolesAndPermissionsService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user-allocation-roles")
@RequiredArgsConstructor
public class UserAllocationRoleController {

  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final EntityManager entityManager;

  @GetMapping("/{allocationId}")
  private UserAllocationRolesResponse getUsersWithAllocationPermissions(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {
    return new UserAllocationRolesResponse(
        rolesAndPermissionsService
            .getAllRolesAndPermissionsForAllocation(allocationId)
            .values()
            .stream()
            .map(UserAllocationRoleRecord::new)
            .collect(Collectors.toList()));
  }

  @PostMapping("/{allocationId}/{granteeId}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  private void createUserAllocationPermission(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @PathVariable("granteeId")
          @Parameter(
              required = true,
              name = "granteeId",
              description = "UserId of the grantee.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> granteeId,
      @RequestBody String newRole) {
    rolesAndPermissionsService.createUserAllocationRole(
        entityManager.getReference(User.class, granteeId),
        entityManager.getReference(Allocation.class, allocationId),
        newRole);
  }

  @PutMapping("/{allocationId}/{granteeId}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  private void updateUserAllocationPermission(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @PathVariable("granteeId")
          @Parameter(
              required = true,
              name = "granteeId",
              description = "UserId of the grantee.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> granteeId,
      @RequestBody String newRole) {
    rolesAndPermissionsService.updateUserAllocationRole(
        entityManager.getReference(User.class, granteeId),
        entityManager.getReference(Allocation.class, allocationId),
        newRole);
  }

  @DeleteMapping("/{allocationId}/{granteeId}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  private void deleteUserAllocationPermission(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @PathVariable("granteeId")
          @Parameter(
              required = true,
              name = "granteeId",
              description = "UserId of the grantee.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<UserId> granteeId) {
    rolesAndPermissionsService.deleteUserAllocationRole(
        entityManager.getReference(User.class, granteeId),
        entityManager.getReference(Allocation.class, allocationId));
  }
}