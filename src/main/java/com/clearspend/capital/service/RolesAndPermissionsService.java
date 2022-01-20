package com.clearspend.capital.service;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.error.ForbiddenException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.security.GlobalRole;
import com.clearspend.capital.data.model.security.UserAllocationRole;
import com.clearspend.capital.data.repository.security.GlobalRoleRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The RolesAndPermissionService maintains globalUserPermissions and permissions related to specific
 * allocations. Checks are made relative to CurrentUser. Roles represent logical groupings of
 * permissions, and permissions are required to perform certain operations. Permissions are
 * enumerated in {@link AllocationPermission} and {@link GlobalUserPermission}. Permissions are
 * related to users through globalUserPermissions. Roles are defined globally and at the business
 * level.
 *
 * <p>This service manages the roles and permission objects rather than a customary service per
 * object type to avoid circular references between this service and the postulated services per
 * object.
 *
 * <p>There are two important defaults to note: A root allocation owner always has Owner role on
 * that allocation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RolesAndPermissionsService {

  private final UserAllocationRoleRepository userAllocationRoleRepository;
  private final GlobalRoleRepository globalRoleRepository;
  private final EntityManager entityManager;

  /**
   * Creates a user's role, adding the given permission to any existing UserAllocationRole
   *
   * @param grantee The ID of the user whose permission is to change
   * @param allocation The allocation ID
   * @param newRole The new role to set
   * @throws ForbiddenException when the current user's permissions are insufficient
   */
  public UserAllocationRole createUserAllocationRole(
      @NonNull User grantee, Allocation allocation, @NonNull String newRole) {
    prepareUserAllocationRoleChange(grantee, allocation, newRole)
        .map(
            r -> {
              throw new InvalidRequestException("Already created");
            });

    UserAllocationRole role =
        userAllocationRoleRepository.save(
            new UserAllocationRole(allocation.getId(), grantee.getId(), newRole));
    entityManager.flush(); // this needs to take effect immediately

    return role;
  }

  /**
   * Updates a user's role, adding the given permission to any existing UserAllocationRole
   *
   * @param grantee The ID of the user whose permission is to change
   * @param allocation The allocation ID
   * @param newRole The new role to set
   * @throws ForbiddenException when the current user's permissions are insufficient
   */
  public UserAllocationRole updateUserAllocationRole(
      @NonNull User grantee, Allocation allocation, @NonNull String newRole) {
    return prepareUserAllocationRoleChange(grantee, allocation, newRole)
        .map(
            r -> {
              r.setRole(newRole);
              UserAllocationRole role = userAllocationRoleRepository.save(r);
              entityManager.flush(); // this needs to take effect immediately
              return role;
            })
        .orElseThrow(() -> new InvalidRequestException("Does not exist."));
  }

  private Optional<UserAllocationRole> prepareUserAllocationRoleChange(
      User grantee, Allocation allocation, String newRole) {
    entityManager.flush();
    Optional<UserAllocationRole> oldRole =
        userAllocationRoleRepository.findByUserIdAndAllocationId(
            grantee.getId(), allocation.getId());
    EnumSet<AllocationPermission> oldPerms =
        oldRole
            .map(
                aur ->
                    userAllocationRoleRepository.getRolePermissions(
                        allocation.getBusinessId(), aur.getRole()))
            .orElse(EnumSet.noneOf(AllocationPermission.class));

    EnumSet<AllocationPermission> newPerms =
        newRole == null
            ? EnumSet.noneOf(AllocationPermission.class)
            : userAllocationRoleRepository.getRolePermissions(allocation.getBusinessId(), newRole);
    EnumSet<AllocationPermission> unchangedPerms = EnumSet.copyOf(oldPerms);
    unchangedPerms.retainAll(newPerms);
    EnumSet<AllocationPermission> allPerms = EnumSet.copyOf(oldPerms);
    allPerms.addAll(newPerms);
    EnumSet<AllocationPermission> changedPerms = EnumSet.copyOf(allPerms);
    changedPerms.removeAll(unchangedPerms);

    EnumSet<AllocationPermission> grantorNeedsPerms = EnumSet.copyOf(changedPerms);
    grantorNeedsPerms.add(AllocationPermission.MANAGE_PERMISSIONS);
    assertUserHasPermission(
        allocation.getId(), grantorNeedsPerms, GlobalUserPermission.ALL_CUSTOMER_SERVICE);

    // Is the grantee archived?
    if (grantee.isArchived()) {
      throw new InvalidRequestException("Grantee is archived");
    }

    // TODO forbid decreasing permissions
    // TODO figure out how to get the grantee's role to check if they can cross business boundary

    /*
        // allow for grantee is a bookkeeper
        if (!allocation.getBusinessId().equals(grantee.getBusinessId())
            && !userHasAnyRole(grantee, GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)) {
          throw new ForbiddenException();
        }
    */
    if (allocation.getOwnerId().equals(grantee.getId())
        && allocation.getParentAllocationId() == null) {
      // The return value confirms the standard
      return Optional.of(new UserAllocationRole(allocation.getId(), grantee.getId(), "Owner"));
    }

    return oldRole;
  }

  /**
   * Get the permissions granted by the given set of globalUserPermissions.
   *
   * @param globalRoles Roles under consideration
   * @return EnumSet of the permissions granted by those globalUserPermissions
   */
  public EnumSet<GlobalUserPermission> getPermissions(Set<String> globalRoles) {
    if (globalRoles == null || globalRoles.isEmpty()) {
      return EnumSet.noneOf(GlobalUserPermission.class);
    }

    return EnumSet.copyOf(
        globalRoleRepository.findAll().stream()
            .filter(gr -> globalRoles.contains(gr.getRole()))
            .map(GlobalRole::getPermissions)
            .flatMap(Stream::of)
            .collect(Collectors.toSet()));
  }

  /**
   * Creates or updates a permission record, adding the given permission to any existing set,
   * creating the permission set if necessary.
   *
   * @param grantee The ID of the user whose permission is to change
   * @param allocation The allocation ID
   * @throws ForbiddenException when the current user's permissions are insufficient
   * @throws RecordNotFoundException if there is nothing to delete, with the exception's id
   *     consisting of a Map of the grantee Id and allocation Id
   */
  public void deleteUserAllocationRole(@NonNull User grantee, Allocation allocation) {
    Optional<UserAllocationRole> doomedRecord =
        prepareUserAllocationRoleChange(grantee, allocation, null);
    doomedRecord
        .map(
            r -> {
              userAllocationRoleRepository.delete(r);
              entityManager.flush();
              return r;
            })
        .orElseThrow(
            () ->
                new RecordNotFoundException(
                    Table.USER_ALLOCATION_ROLE,
                    Map.of("granteeUserId", grantee.getId(), "allocationId", allocation.getId())));
  }

  /**
   * @param allocationId The allocation for which permission is requested, null if the permission is
   *     global
   * @param allocationPermission Each allocation permission that would be sufficient
   * @param globalUserPermissions Each given user permission is sufficient
   * @throws ForbiddenException when user permissions are insufficient
   */
  public void assertUserHasPermission(
      TypedId<AllocationId> allocationId,
      @NonNull EnumSet<AllocationPermission> allocationPermission,
      EnumSet<GlobalUserPermission> globalUserPermissions)
      throws ForbiddenException {
    entityManager.flush();
    CurrentUser user = CurrentUser.get();
    if (!userAllocationRoleRepository.userHasPermission(
        user.businessId(),
        allocationId,
        user.userId(),
        user.roles(),
        allocationPermission,
        globalUserPermissions)) {
      throw new ForbiddenException(
          List.of(
                  user.businessId(),
                  allocationId,
                  user.userId(),
                  user.roles(),
                  allocationPermission,
                  globalUserPermissions)
              .toString());
    }
  }

  public UserRolesAndPermissions getUserRolesAndPermissionsForAllocation(
      TypedId<AllocationId> allocationId) {
    CurrentUser currentUser = CurrentUser.get();
    assertUserHasPermission(
        allocationId,
        EnumSet.of(com.clearspend.capital.data.model.enums.AllocationPermission.MANAGE_PERMISSIONS),
        GlobalUserPermission.ALL_CUSTOMER_SERVICE);
    return userAllocationRoleRepository.getUserPermissionAtAllocation(
        currentUser.businessId(), allocationId, currentUser.userId(), currentUser.roles());
  }

  public Map<TypedId<UserId>, UserRolesAndPermissions> getAllRolesAndPermissionsForAllocation(
      TypedId<AllocationId> allocationId) {
    CurrentUser currentUser = CurrentUser.get();
    Map<TypedId<UserId>, UserRolesAndPermissions> permissionsMap =
        userAllocationRoleRepository.getActiveUsersWithAllocationPermission(
            currentUser.businessId(), allocationId);

    if (permissionsMap
            .get(currentUser.userId())
            .allocationPermissions()
            .contains(AllocationPermission.READ)
        || getPermissions(currentUser.roles()).contains(GlobalUserPermission.GLOBAL_READ)) {
      return permissionsMap;
    }

    throw new ForbiddenException();
  }

  void setUserAllocationRole(
      TypedId<AllocationId> allocationId, TypedId<UserId> grantee, String role) {
    assertUserHasPermission(
        allocationId,
        EnumSet.of(AllocationPermission.MANAGE_PERMISSIONS),
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE));
    // TODO assert that permissions are not lowered
    userAllocationRoleRepository.save(new UserAllocationRole(allocationId, grantee, role));
  }

  // TODO accessible businesses for a user (bookkeeper, customer service)
}
