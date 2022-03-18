package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.data.model.security.DefaultRoles;

public record RootAllocationRole(String role) implements PermissionValidationRole {
  public static final RootAllocationRole ALLOCATION_ADMIN =
      new RootAllocationRole(DefaultRoles.ALLOCATION_ADMIN);
  public static final RootAllocationRole ALLOCATION_MANAGER =
      new RootAllocationRole(DefaultRoles.ALLOCATION_MANAGER);
  public static final RootAllocationRole ALLOCATION_EMPLOYEE =
      new RootAllocationRole(DefaultRoles.ALLOCATION_EMPLOYEE);
  public static final RootAllocationRole ALLOCATION_VIEW_ONLY =
      new RootAllocationRole(DefaultRoles.ALLOCATION_VIEW_ONLY);
}
