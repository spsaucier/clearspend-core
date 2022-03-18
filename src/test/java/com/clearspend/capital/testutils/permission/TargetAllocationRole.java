package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.data.model.security.DefaultRoles;

public record TargetAllocationRole(String role) implements PermissionValidationRole {
  public static final TargetAllocationRole ALLOCATION_ADMIN =
      new TargetAllocationRole(DefaultRoles.ALLOCATION_ADMIN);
  public static final TargetAllocationRole ALLOCATION_MANAGER =
      new TargetAllocationRole(DefaultRoles.ALLOCATION_MANAGER);
  public static final TargetAllocationRole ALLOCATION_EMPLOYEE =
      new TargetAllocationRole(DefaultRoles.ALLOCATION_EMPLOYEE);
  public static final TargetAllocationRole ALLOCATION_VIEW_ONLY =
      new TargetAllocationRole(DefaultRoles.ALLOCATION_VIEW_ONLY);
}
