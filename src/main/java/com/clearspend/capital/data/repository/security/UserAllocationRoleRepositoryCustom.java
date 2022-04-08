package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  Optional<UserRolesAndPermissions> getUserPermissionAtBusiness(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, Set<String> userGlobalRoles);

  void deleteAllForGranteeByAllocationId(
      TypedId<UserId> grantee, List<TypedId<AllocationId>> allocations);

  /**
   * Return the user's permission for a particular allocation, with allocationRoles and permissions
   * both global and allocation-lavel
   *
   * @param businessId The business to match with the allocation
   * @param allocationId The allocation of interest
   * @param userId The user whose permission is being interrogated
   * @return The effective user permissions at the given level
   */
  Optional<UserRolesAndPermissions> getUserPermissionAtAllocation(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Set<String> userGlobalRoles);

  /**
   * Delete roles on descendants of the given allocation which have less than all the permissions
   * that the given role has, for use in enforcing no lowered permissions below the current
   * allocation.
   *
   * @param granteeUserId the userId whose roles will be modified
   * @param allocationId the allocation under scrutiny
   * @param referenceRole the role to match
   */
  void deleteLesserAndEqualRolesBelow(
      TypedId<UserId> granteeUserId, TypedId<AllocationId> allocationId, String referenceRole);

  /**
   * Retrieves all possible permissions for the specified user within the specified business,
   * factoring in global permissions.
   *
   * @param userId the User to get permissions for.
   * @param businessId the Business the user is trying to access.
   * @param globalRoles any global roles associated with the specified user.
   * @return all possible permissions.
   */
  List<UserRolesAndPermissions> findAllByUserIdAndBusinessId(
      final TypedId<UserId> userId,
      final TypedId<BusinessId> businessId,
      final Set<String> globalRoles);

  /**
   * Retrieves all possible permissions for the specified user and allocation, factoring in global
   * permissions.
   *
   * @param userId the User to get permissions for.
   * @param allocationId the Allocation the user is trying to access.
   * @param globalRoles any global roles associated with the specified user.
   * @return all possible permissions
   */
  Optional<UserRolesAndPermissions> findAllByUserIdAndAllocationId(
      final TypedId<UserId> userId,
      final TypedId<AllocationId> allocationId,
      final Set<String> globalRoles);

  /**
   * Retrieves all possible permissions for the specified user within the business & allocation,
   * factoring in global permissions. The Business ID, Allocation ID, or both must be present, and
   * the Allocation ID must be within the Business ID if both are present. This will be enforced by
   * the SQL query.
   *
   * @param userId the User to get permissions for.
   * @param businessId the Business the user is trying to access.
   * @param allocationId the Allocation the user is trying to access.
   * @param globalRoles any global roles associated with the user.
   * @return all possible permissions.
   */
  Optional<UserRolesAndPermissions> findAllByUserIdAndBusinessIdAndAllocationId(
      final TypedId<UserId> userId,
      final TypedId<BusinessId> businessId,
      final TypedId<AllocationId> allocationId,
      final Set<String> globalRoles);
}
