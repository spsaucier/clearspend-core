package com.clearspend.capital.service;

import static com.clearspend.capital.data.model.security.DefaultRoles.GLOBAL_BOOKKEEPER;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.error.ForbiddenException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.model.security.GlobalRole;
import com.clearspend.capital.data.model.security.UserAllocationRole;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.security.AllocationRolePermissionsRepository;
import com.clearspend.capital.data.repository.security.GlobalRoleRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import com.clearspend.capital.service.FusionAuthService.FusionAuthRoleAdministrator;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserAccessor;
import com.clearspend.capital.service.FusionAuthService.RoleChange;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
  private final FusionAuthService fusionAuthService;
  private final AllocationRepository allocationRepository;
  private final EntityManager entityManager;
  private final AllocationRolePermissionsRepository allocationRolePermissionsRepository;

  /**
   * Creates a user's role, adding the given permission to any existing UserAllocationRole
   *
   * @param grantee The ID of the user whose permission is to change
   * @param allocation The allocation ID
   * @param newRole The new role to set
   * @throws ForbiddenException when the current user's permissions are insufficient
   */
  public com.clearspend.capital.data.model.security.UserAllocationRole createUserAllocationRole(
      @NonNull User grantee, Allocation allocation, @NonNull String newRole) {

    prepareUserAllocationRoleChange(grantee, allocation, newRole)
        .ifPresent(
            r -> {
              throw new InvalidRequestException("Already created");
            });

    com.clearspend.capital.data.model.security.UserAllocationRole role =
        userAllocationRoleRepository.save(
            new com.clearspend.capital.data.model.security.UserAllocationRole(
                allocation.getId(), grantee.getId(), newRole));
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
  public com.clearspend.capital.data.model.security.UserAllocationRole updateUserAllocationRole(
      @NonNull User grantee, Allocation allocation, @NonNull String newRole) {
    return prepareUserAllocationRoleChange(grantee, allocation, newRole)
        .map(
            r -> {
              r.setRole(newRole);
              com.clearspend.capital.data.model.security.UserAllocationRole role =
                  userAllocationRoleRepository.save(r);
              entityManager.flush(); // this needs to take effect immediately
              return role;
            })
        .orElseThrow(() -> new InvalidRequestException("Does not exist."));
  }

  @FusionAuthUserAccessor(
      reviewer = "jscarbor",
      explanation = "This is the service for managing user roles")
  private Optional<com.clearspend.capital.data.model.security.UserAllocationRole>
      prepareUserAllocationRoleChange(User grantee, Allocation allocation, String newRole) {
    entityManager.flush();

    if (grantee.getBusinessId().equals(allocation.getBusinessId())
        && grantee.getType().equals(UserType.BUSINESS_OWNER)) {
      // The return value confirms the standard
      throw new InvalidRequestException("Business owners are always admins");
    }

    final Set<String> granteeGlobalRoles =
        fusionAuthService.getUserRoles(UUID.fromString(grantee.getSubjectRef()));
    if (!grantee.getBusinessId().equals(allocation.getBusinessId())
        && !granteeGlobalRoles.contains(GLOBAL_BOOKKEEPER)) {
      throw new InvalidRequestException("Only bookkeepers can cross business boundaries");
    }

    UserRolesAndPermissions oldEffectivePermissions =
        userAllocationRoleRepository.getUserPermissionAtAllocation(
            grantee.getBusinessId(), allocation.getId(), grantee.getId(), granteeGlobalRoles);

    EnumSet<AllocationPermission> newPerms =
        newRole == null
            ? EnumSet.noneOf(AllocationPermission.class)
            : userAllocationRoleRepository.getRolePermissions(allocation.getBusinessId(), newRole);

    // Establish the minimum permission applicable
    //
    // Some rules:
    // - Every allocation owner who is not a business owner has explicit permission
    //     of Manager or Admin at the top allocation they own.
    // - Allocation owners do not have explicit permissions on child allocations.
    // - Whenever allocation owner permissions are set (or owners changed), check for
    //     descendant allocations with permissions and update according to the above
    // - Permissions cannot be taken away lower in the tree
    if (oldEffectivePermissions != null) {
      // See where they got the existing role from
      TypedId<AllocationId> foundRoleAllocationId = oldEffectivePermissions.allocationId();

      List<TypedId<AllocationId>> allocationIds =
          new ArrayList<>(allocation.getAncestorAllocationIds());
      allocationIds.add(allocation.getId());
      Collections.reverse(allocationIds); // sort leaf to root
      // TODO see that this check is working its way up the tree,

      // See if they own any of the allocations with the role
      List<TypedId<AllocationId>> maybeOwnedAllocationIds =
          allocationIds.subList(allocationIds.indexOf(foundRoleAllocationId), allocationIds.size());
      Map<TypedId<AllocationId>, Allocation> ancestorAllocations =
          allocationRepository.findAllById(maybeOwnedAllocationIds).stream()
              .collect(Collectors.toMap(Allocation::getId, a -> a));
      Optional<Allocation> ownedAllocationOptional =
          maybeOwnedAllocationIds.stream()
              .map(ancestorAllocations::get)
              .filter(a -> a.getOwnerId().equals(grantee.getId()))
              .findFirst();

      boolean isAllocationOwner = ownedAllocationOptional.isPresent();
      EnumSet<AllocationPermission> requiredPermissions =
          EnumSet.noneOf(AllocationPermission.class);
      if (isAllocationOwner) {
        requiredPermissions.addAll(
            userAllocationRoleRepository.getRolePermissions(
                allocation.getBusinessId(), DefaultRoles.ALLOCATION_MANAGER));
      }
      if (allocation.getParentAllocationId() != null) {
        requiredPermissions.addAll(
            userAllocationRoleRepository
                .getUserPermissionAtAllocation(
                    grantee.getBusinessId(),
                    allocation.getParentAllocationId(),
                    grantee.getId(),
                    granteeGlobalRoles)
                .allocationPermissions());
      }

      // Enforce the minimum
      if (!newPerms.containsAll(requiredPermissions)) {
        throw new InvalidRequestException("Cannot reduce permissions");
      }
    }

    EnumSet<AllocationPermission> oldPerms =
        oldEffectivePermissions == null
            ? EnumSet.noneOf(AllocationPermission.class)
            : oldEffectivePermissions.allocationPermissions();
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

    // Permission granted ====================================================

    // TODO forbid decreasing inherited permissions
    // remove any lower permissions
    if (newRole != null) {
      userAllocationRoleRepository.deleteLesserAndEqualRolesBelow(
          grantee.getId(), allocation.getId(), newRole);
    }

    return userAllocationRoleRepository.findByUserIdAndAllocationId(
        grantee.getId(), allocation.getId());
  }

  /**
   * Get the permissions granted by the given set of globalUserPermissions.
   *
   * @param globalRoles Roles under consideration
   * @return EnumSet of the permissions granted by those globalUserPermissions
   */
  public EnumSet<GlobalUserPermission> getGlobalPermissions(Set<String> globalRoles) {
    if (globalRoles == null || globalRoles.isEmpty()) {
      return EnumSet.noneOf(GlobalUserPermission.class);
    }

    return EnumSet.copyOf(
        globalRoleRepository.findAll().stream()
            .filter(gr -> globalRoles.contains(gr.getRoleName()))
            .map(GlobalRole::getPermissions)
            .filter(perms -> perms.length > 0)
            .flatMap(Stream::of)
            .collect(Collectors.toSet()));
  }

  /**
   * Creates or updates a permission record, adding the given permission to any existing set,
   * creating the permission set if necessary.
   *
   * @param allocation The allocation ID
   * @param grantee The ID of the user whose permission is to change
   * @throws ForbiddenException when the current user's permissions are insufficient
   * @throws RecordNotFoundException if there is nothing to delete, with the exception's id
   *     consisting of a Map of the grantee Id and allocation Id
   */
  public void deleteUserAllocationRole(Allocation allocation, @NonNull User grantee) {
    Optional<com.clearspend.capital.data.model.security.UserAllocationRole> doomedRecord =
        prepareUserAllocationRoleChange(grantee, allocation, null);
    UserAllocationRole record =
        doomedRecord.orElseThrow(
            () ->
                new RecordNotFoundException(
                    Table.USER_ALLOCATION_ROLE,
                    Map.of("granteeUserId", grantee.getId(), "allocationId", allocation.getId())));

    userAllocationRoleRepository.delete(record);
    userAllocationRoleRepository.flush();
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
          Map.of(
                  "businessId",
                  user.businessId(),
                  "allocationId",
                  allocationId,
                  "userId",
                  user.userId(),
                  "userRoles",
                  user.roles(),
                  "allocationPermissions",
                  allocationPermission,
                  "globalUserPermissions",
                  globalUserPermissions)
              .toString());
    }
  }

  public UserRolesAndPermissions getUserRolesAndPermissionsForAllocation(
      TypedId<AllocationId> allocationId) {
    CurrentUser currentUser = CurrentUser.get();
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
        || getGlobalPermissions(currentUser.roles()).contains(GlobalUserPermission.GLOBAL_READ)) {
      return permissionsMap;
    }

    throw new ForbiddenException();
  }

  @FusionAuthRoleAdministrator(
      reviewer = "jscarbor",
      explanation = "This is the service for managing user roles")
  public boolean grantGlobalRole(@NonNull TypedId<UserId> grantee, @NonNull String role) {
    return grantGlobalRole(entityManager.getReference(User.class, grantee), role);
  }

  @FusionAuthRoleAdministrator(
      reviewer = "jscarbor",
      explanation = "This is the service for managing user roles")
  public boolean grantGlobalRole(@NonNull User grantee, @NonNull String role) {
    authorizeGlobalRoleChange(role);
    return fusionAuthService.changeUserRole(RoleChange.GRANT, grantee.getSubjectRef(), role);
  }

  @FusionAuthRoleAdministrator(
      reviewer = "jscarbor",
      explanation = "This is the service for managing user roles")
  public boolean revokeGlobalRole(@NonNull TypedId<UserId> grantee, @NonNull String role) {
    authorizeGlobalRoleChange(role);
    return fusionAuthService.changeUserRole(
        RoleChange.REVOKE, entityManager.getReference(User.class, grantee).getSubjectRef(), role);
  }

  private void authorizeGlobalRoleChange(@NonNull String role) {
    List<GlobalRole> globalRoles = globalRoleRepository.findAll();
    if (globalRoles.stream().noneMatch(r -> r.getRoleName().equals(role))) {
      throw new IllegalArgumentException("role=%s".formatted(role));
    }
    Set<String> grantorRoles = CurrentUser.get().roles();
    EnumSet<GlobalUserPermission> grantorPermissions = getGlobalPermissions(grantorRoles);
    EnumSet<GlobalUserPermission> grantorNeedsPermissions = getGlobalPermissions(Set.of(role));
    grantorNeedsPermissions.add(GlobalUserPermission.CUSTOMER_SERVICE);
    grantorNeedsPermissions.removeAll(
        EnumSet.of(
            GlobalUserPermission.BATCH_ONBOARD, GlobalUserPermission.CROSS_BUSINESS_BOUNDARY));
    if (!grantorPermissions.containsAll(grantorNeedsPermissions)) {
      throw new ForbiddenException("Grantor does not have sufficient permission.");
    }
  }

  public Set<String> getGlobalRoles(@NonNull TypedId<UserId> userId) {
    return getGlobalRoles(entityManager.getReference(User.class, userId));
  }

  @FusionAuthUserAccessor(
      reviewer = "jscarbor",
      explanation = "This is the service for managing user roles")
  public Set<String> getGlobalRoles(@NonNull User user) {
    return fusionAuthService.getUserRoles(UUID.fromString(user.getSubjectRef()));
  }

  public UserRolesAndPermissions getUserRolesAndPermissionsAtRootAllocation(
      TypedId<BusinessId> businessId) {
    CurrentUser user = CurrentUser.get();
    return userAllocationRoleRepository.getUserPermissionAtBusiness(
        businessId, user.userId(), user.roles());
  }

  public void ensureMinimumAllocationPermissions(
      User user, Allocation allocation, String defaultRole) {
    if (user.getType().equals(UserType.EMPLOYEE)) {
      UserRolesAndPermissions existingRole =
          getUserRolesAndPermissionsForAllocation(allocation.getId());
      EnumSet<AllocationPermission> minimumPermissions =
          EnumSet.copyOf(
              Arrays.asList(
                  allocationRolePermissionsRepository
                      .findAllocationRolePermissionsByBusiness(allocation.getBusinessId())
                      .stream()
                      .filter(r -> r.getRoleName().equals(defaultRole))
                      .map(AllocationRolePermissions::getPermissions)
                      .findFirst()
                      .orElseThrow()));
      if (existingRole == null) {
        createUserAllocationRole(user, allocation, defaultRole);
      } else {
        if (!existingRole.allocationPermissions().containsAll(minimumPermissions)) {
          if (existingRole.inherited()) {
            createUserAllocationRole(user, allocation, defaultRole);
          } else {
            updateUserAllocationRole(user, allocation, defaultRole);
          }
        }
      }
    }
  }

  // TODO list businesses available to a user (bookkeeper, customer service)
}
