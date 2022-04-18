package com.clearspend.capital.service.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;

public class UserRolesAndPermissionsCache {

  /**
   * If no permissions are found during the query, this placeholder is stored in the cache to
   * short-circuit any future lookups for that same allocation/business/etc during the operation.
   */
  private static final UserRolesAndPermissions EMPTY_PERMISSIONS =
      new UserRolesAndPermissions(
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          false,
          null,
          EnumSet.noneOf(AllocationPermission.class),
          EnumSet.noneOf(GlobalUserPermission.class));

  private final Map<TypedId<BusinessId>, List<UserRolesAndPermissions>> businessPermissionsMap =
      new HashMap<>();
  private final Map<TypedId<AllocationId>, UserRolesAndPermissions> allocationPermissionsMap =
      new HashMap<>();
  private final List<FailedPermissions> failedPermissions = new ArrayList<>();

  public void storeFailedPermissions(
      @NonNull final PermissionEvaluationIds permissionEvaluationIds,
      @NonNull final String requiredPermissions,
      @Nullable final UserRolesAndPermissions userRolesAndPermissions,
      @Nullable final OverlapPermissions overlapPermissions) {
    failedPermissions.add(
        new FailedPermissions(
            permissionEvaluationIds, requiredPermissions, userRolesAndPermissions));
  }

  public void storeFailedPermissions(
      @NonNull final PermissionEvaluationIds permissionEvaluationIds,
      @NonNull final String requiredPermissions) {
    storeFailedPermissions(permissionEvaluationIds, requiredPermissions, null, null);
  }

  public List<FailedPermissions> getFailedPermissions() {
    return failedPermissions;
  }

  /**
   * Get cached permissions by Business ID, if any exist. All permissions for all allocations the
   * user has within the business will be returned, or nothing at all if no permissions have been
   * queried yet.
   *
   * @param businessId the Business ID.
   * @return the permissions for the business, if there are any.
   */
  public Optional<List<UserRolesAndPermissions>> getPermissionsForBusiness(
      final TypedId<BusinessId> businessId) {
    return Optional.ofNullable(businessPermissionsMap.get(businessId));
  }

  /**
   * Get cached permissions by Allocation ID, if any exist. The permissions will be returned, or
   * nothing at all if no permissions have been queried yet.
   *
   * @param allocationId the Allocation ID.
   * @return the permissions for the allocation, if there are any.
   */
  public Optional<UserRolesAndPermissions> getPermissionsForAllocation(
      final TypedId<AllocationId> allocationId) {
    return Optional.ofNullable(allocationPermissionsMap.get(allocationId));
  }

  /**
   * Get the global permissions. This cache only exists for a single user during a single API call.
   * Global permissions are also stored in every UserRolesAndPermissions record, and regardless of
   * caching by allocation or business, there will be cached items (if any) in the allocation map.
   * Therefore a single record from the allocation permissions map is returned to access any cached
   * global permissions.
   *
   * @return a UserRolesAndPermissions record that has global permissions.
   */
  public Optional<UserRolesAndPermissions> getPermissionsForGlobal() {
    if (allocationPermissionsMap.isEmpty()) {
      return Optional.empty();
    }
    return allocationPermissionsMap.values().stream()
        .filter(userPermissions -> !EMPTY_PERMISSIONS.equals(userPermissions))
        .findFirst()
        // If we are trying to only return global permissions, we should suppress the allocation
        // permissions here to avoid accidentally giving too much back
        .map(
            permissions ->
                permissions.withAllocationPermissions(EnumSet.noneOf(AllocationPermission.class)));
  }

  /**
   * Cache all the permissions for a single allocation. Because only one allocation is being
   * provided, it is only cached by allocation ID and not by business ID. This way future queries
   * for permissions by Business ID need to go to the database to ensure completeness.
   *
   * @param permissions the permissions to cache.
   * @return the permissions argument.
   */
  public UserRolesAndPermissions cachePermissionsForAllocation(
      @Nullable final TypedId<AllocationId> allocationId,
      @Nullable final UserRolesAndPermissions permissions) {
    if (permissions == null) {
      allocationPermissionsMap.put(allocationId, EMPTY_PERMISSIONS);
      return EMPTY_PERMISSIONS;
    }
    allocationPermissionsMap.put(permissions.allocationId(), permissions);
    return permissions;
  }

  /**
   * Cache all permissions for a business. This expects all allocations with permissions under a
   * given business for the user in question to be provided. The permissions are stored in two maps,
   * one by Business ID and one by Allocation ID, to maximize the ability of future calls to
   * leverage the cached data.
   *
   * @param permissions the permissions to cache.
   * @return the permissions argument.
   */
  public List<UserRolesAndPermissions> cachePermissionsForBusiness(
      @Nullable final TypedId<BusinessId> businessId,
      @Nullable final List<UserRolesAndPermissions> permissions) {
    if (permissions == null || permissions.isEmpty()) {
      businessPermissionsMap.put(businessId, List.of(EMPTY_PERMISSIONS));
      return permissions;
    }

    businessPermissionsMap.put(businessId, permissions);

    // Organize the permissions by allocation ID and store them for easy retrieval
    final Map<TypedId<AllocationId>, UserRolesAndPermissions> permissionsByAllocation =
        permissions.stream()
            .collect(Collectors.toMap(UserRolesAndPermissions::allocationId, Function.identity()));
    allocationPermissionsMap.putAll(permissionsByAllocation);
    return permissions;
  }
}
