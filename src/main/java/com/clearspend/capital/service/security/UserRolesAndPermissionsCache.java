package com.clearspend.capital.service.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserRolesAndPermissionsCache {

  private final Map<TypedId<BusinessId>, UserRolesAndPermissions> permissionsByBusiness =
      new HashMap<>();
  private final Map<TypedId<AllocationId>, UserRolesAndPermissions> permissionsByAllocation =
      new HashMap<>();

  public Optional<UserRolesAndPermissions> getPermissionsForBusiness(
      TypedId<BusinessId> businessId) {
    return Optional.ofNullable(permissionsByBusiness.get(businessId));
  }

  public Optional<UserRolesAndPermissions> getPermissionsForAllocation(
      TypedId<AllocationId> allocationId) {
    return Optional.ofNullable(permissionsByAllocation.get(allocationId));
  }

  public UserRolesAndPermissions cachePermissions(UserRolesAndPermissions permissions) {
    permissionsByAllocation.put(permissions.allocationId(), permissions);
    if (permissions.ancestorAllocationIds().isEmpty()) {
      permissionsByBusiness.put(permissions.businessId(), permissions);
    }
    return permissions;
  }
}
