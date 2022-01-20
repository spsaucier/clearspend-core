package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public interface UserAllocationRoleRepositoryCustom {

  Map<TypedId<UserId>, UserRolesAndPermissions> getActiveUsersWithAllocationPermission(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId);

  /**
   * Boolean to check if the user has permission. This method is at the repository level instead of
   * the service level to facilitate the DB performing the final evaluation.
   *
   * @param businessId to match with the allocation
   * @param allocationId the allocation under consideration
   * @param userId the user whose permission is being interrogated
   * @param userGlobalRoles the roles the user has, per the JWT
   * @param allocationPermissions the needed permission at allocation level
   * @param globalUserPermissions the needed permissions at global level
   * @return true if the user has any of the permissions at allocation or global level, false
   *     otherwise.
   */
  boolean userHasPermission(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Set<String> userGlobalRoles,
      EnumSet<AllocationPermission> allocationPermissions,
      EnumSet<GlobalUserPermission> globalUserPermissions);

  /**
   * Return the user's permission for a particular allocation, with allocationRoles and permissions
   * both global and allocation-lavel
   *
   * @param businessId The business to match with the allocation
   * @param userId The user whose permission is being interrogated
   * @param userGlobalRoles User roles from CurrentUser
   * @return The effective user permissions at the given level
   */
  UserRolesAndPermissions getUserPermissionAtBusiness(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, Set<String> userGlobalRoles);

  /**
   * Return the user's permission for a particular allocation, with allocationRoles and permissions
   * both global and allocation-lavel
   *
   * @param businessId The business to match with the allocation
   * @param allocationId The allocation of interest
   * @param userId The user whose permission is being interrogated
   * @return The effective user permissions at the given level
   */
  UserRolesAndPermissions getUserPermissionAtAllocation(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Set<String> userGlobalRoles);

  default EnumSet<AllocationPermission> getRolePermissions(
      TypedId<BusinessId> businessId, String role) {
    return getRolePermissions(businessId, Set.of(role));
  }

  EnumSet<AllocationPermission> getRolePermissions(
      TypedId<BusinessId> businessId, Set<String> roles);
}
