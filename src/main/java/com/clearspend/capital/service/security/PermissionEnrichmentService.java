package com.clearspend.capital.service.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.service.RolesAndPermissionsService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * This service handles evaluating permissions. It takes in arguments provided by various security
 * annotation expressions that includes one or more requested permissions, and then evaluates
 * whether or not the user has those permissions. At its core this leverages the powerful SQL logic
 * in Postgres to do most of the work,
 * (https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2107768940/Permission+Retrieval+in+Capital-Core+SQL+Functions)
 * but it also provides an additional layer on top of that. It determines which arguments to pass to
 * the SQL functions, caches the results to minimize database calls, and does some filtering of what
 * is returned.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionEnrichmentService {

  private final RolesAndPermissionsService rolesAndPermissionsService;

  public boolean evaluatePermission(
      @NonNull final Authentication authentication,
      @NonNull PermissionEvaluationIds permissionEvaluationIds,
      @NonNull final String permissions) {
    final UserRolesAndPermissionsCache cache =
        (UserRolesAndPermissionsCache) authentication.getDetails();
    return getPermissions(cache, permissionEvaluationIds)
        .map(
            userPermissions ->
                doEvaluatePermissions(
                    permissionEvaluationIds.userId(), userPermissions, permissions))
        .orElse(false);
  }

  private record LastCall(String className, String methodName) {}

  private boolean doEvaluatePermissions(
      @Nullable final TypedId<UserId> userId,
      @NonNull final UserRolesAndPermissions userPermissions,
      @NonNull final String permissions) {
    log.trace("User Permissions: {}", userPermissions);
    final RequiredPermissions overlapPermissions = resolvePermission(String.valueOf(permissions));
    log.trace("Overlap permissions: {}", overlapPermissions);
    overlapPermissions.allocationPermissions.retainAll(userPermissions.allocationPermissions());
    overlapPermissions.globalUserPermissions.retainAll(userPermissions.globalUserPermissions());

    if (log.isInfoEnabled()) {
      final EnumSet<GlobalUserPermission> customerServiceUsed =
          GlobalUserPermission.ALL_CUSTOMER_SERVICE.clone();

      customerServiceUsed.retainAll(overlapPermissions.globalUserPermissions);
      log.trace("Customer service permissions: {}", customerServiceUsed);

      if (overlapPermissions.allocationPermissions.isEmpty() && !customerServiceUsed.isEmpty()) {
        final LastCall lastCall =
            Arrays.stream(Thread.currentThread().getStackTrace())
                .filter(
                    ste ->
                        ste.getClassName().contains("clearspend")
                            && !ste.getClassName().equals(this.getClass().getName()))
                .findFirst()
                .map(ste -> new LastCall(ste.getClassName(), ste.getMethodName()))
                .orElse(new LastCall("Unknown", "Unknown"));
        log.info(
            "Customer service action {}.{}() by user {}",
            lastCall.className(),
            lastCall.methodName(),
            CurrentUser.getUserId());
      }
    }

    return hasPermissions(overlapPermissions)
        // For VIEW_OWN:
        // 1) If there are any other valid permissions besides VIEW_OWN, we don't need to evaluate
        // it. They already have permission
        // 2) If the only permission is VIEW_OWN, then there must be a non-null User ID that matches
        // the CurrentUser.userId().
        // This is because the User ID passed to this method is the User ID of the resource being
        // retrieved and it determines ownership
        && (!isOnlyPermissionViewOwn(overlapPermissions) || isAllowedViewOwn(userId));
  }

  private boolean hasPermissions(@NonNull final RequiredPermissions overlapPermissions) {
    return !(overlapPermissions.allocationPermissions.isEmpty()
        && overlapPermissions.globalUserPermissions.isEmpty());
  }

  private boolean isOnlyPermissionViewOwn(@NonNull final RequiredPermissions overlapPermissions) {
    return overlapPermissions.globalUserPermissions.isEmpty()
        && overlapPermissions.allocationPermissions().size() == 1
        && overlapPermissions.allocationPermissions.contains(AllocationPermission.VIEW_OWN);
  }

  private boolean isAllowedViewOwn(@Nullable final TypedId<UserId> userId) {
    return CurrentUser.getUserId().equals(userId);
  }

  private Optional<UserRolesAndPermissions> getPermissions(
      @NonNull final UserRolesAndPermissionsCache cache,
      @NonNull final PermissionEvaluationIds permissionEvaluationIds) {
    return switch (permissionEvaluationIds.getEvaluationType()) {
      case BUSINESS_AND_ALLOCATION_ID -> getPermissionsForBusinessAndAllocation(
          cache, permissionEvaluationIds.businessId(), permissionEvaluationIds.allocationId());
      case BUSINESS_ID_ONLY -> getRootAllocationPermissions(
          cache, permissionEvaluationIds.businessId());
      case ALLOCATION_ID_ONLY -> getPermissionsForAllocation(
          cache, permissionEvaluationIds.allocationId());
      case NO_IDS -> getPermissionsForGlobal(cache);
    };
  }

  /**
   * Load all permissions for the business that owns the CurrentUser in order to get the global
   * permissions for that user. Prevents multiple queries if the user's business is involved in any
   * other expressions. Also short-circuits lookup if the user has any permissions already loaded
   * into the cache.
   *
   * <p>Because the scenario that leads to getting global permissions is a little vague (ie, this is
   * called when no business or allocation id is provided), the allocation permissions from the
   * record are cleared prior to it being returned so that only global permissions are provided.
   */
  private Optional<UserRolesAndPermissions> getPermissionsForGlobal(
      @NonNull final UserRolesAndPermissionsCache cache) {
    return cache
        .getPermissionsForGlobal()
        .or(
            () -> {
              log.trace(
                  "Permissions Cache Miss. Querying by Business ({}) for User ({})",
                  CurrentUser.getBusinessId(),
                  CurrentUser.getUserId());
              // Will cache an empty list which will short-circuit this query from running
              // again
              cache.cachePermissionsForBusiness(
                  CurrentUser.getBusinessId(),
                  rolesAndPermissionsService.findAllByUserIdAndBusinessId(
                      CurrentUser.getUserId(),
                      CurrentUser.getBusinessId(),
                      CurrentUser.getRoles()));
              return cache.getPermissionsForGlobal();
            });
  }

  /** Load all permissions for the specified allocation into memory. */
  private Optional<UserRolesAndPermissions> getPermissionsForAllocation(
      @NonNull final UserRolesAndPermissionsCache cache,
      @NonNull final TypedId<AllocationId> allocationId) {
    return cache
        .getPermissionsForAllocation(allocationId)
        .or(
            () -> {
              log.trace(
                  "Permissions Cache Miss. Querying by Allocation ({}) for User ({})",
                  allocationId,
                  CurrentUser.getUserId());
              return rolesAndPermissionsService
                  .findAllByUserIdAndAllocationId(
                      CurrentUser.getUserId(), allocationId, CurrentUser.getRoles())
                  .map(
                      permissions -> cache.cachePermissionsForAllocation(allocationId, permissions))
                  // Store empty permissions if nothing returned so we don't try the query again
                  .or(() -> Optional.of(cache.cachePermissionsForAllocation(allocationId, null)));
            });
  }

  /** Load all permissions for business into memory, then just return the root allocation. */
  private Optional<UserRolesAndPermissions> getRootAllocationPermissions(
      @NonNull final UserRolesAndPermissionsCache cache,
      @NonNull final TypedId<BusinessId> businessId) {
    return cache
        .getPermissionsForBusiness(businessId)
        .orElseGet(
            () -> {
              log.trace(
                  "Permissions Cache Miss. Querying by Business ({}) for User ({})",
                  businessId,
                  CurrentUser.getUserId());
              return // Will cache empty list if no permissions found to short-circuit future
              // queries
              cache.cachePermissionsForBusiness(
                  businessId,
                  rolesAndPermissionsService.findAllByUserIdAndBusinessId(
                      CurrentUser.getUserId(), businessId, CurrentUser.getRoles()));
            })
        .stream()
        .filter(userPermissions -> userPermissions.parentAllocationId() == null)
        .findFirst();
  }

  /**
   * Load all permissions for the specified allocation within the specified business into memory.
   */
  private Optional<UserRolesAndPermissions> getPermissionsForBusinessAndAllocation(
      @NonNull final UserRolesAndPermissionsCache cache,
      @NonNull final TypedId<BusinessId> businessId,
      @NonNull final TypedId<AllocationId> allocationId) {
    return cache
        .getPermissionsForAllocation(allocationId)
        .or(
            () -> {
              log.trace(
                  "Permissions Cache Miss. Querying by Business ({}) & Allocation ({}) for User ({})",
                  businessId,
                  allocationId,
                  CurrentUser.getUserId());
              return rolesAndPermissionsService
                  .findAllByUserIdAndBusinessIdAndAllocationId(
                      CurrentUser.getUserId(), businessId, allocationId, CurrentUser.getRoles())
                  .map(
                      permissions -> cache.cachePermissionsForAllocation(allocationId, permissions))
                  // Store empty permissions if nothing returned so we don't try the query again
                  .or(() -> Optional.of(cache.cachePermissionsForAllocation(allocationId, null)));
            });
  }

  private record RequiredPermissions(
      EnumSet<AllocationPermission> allocationPermissions,
      EnumSet<GlobalUserPermission> globalUserPermissions) {}

  private static final Map<String, AllocationPermission> allocationPermissions =
      EnumSet.allOf(AllocationPermission.class).stream()
          .collect(Collectors.toMap(AllocationPermission::name, e -> e));

  private static final Map<String, GlobalUserPermission> globalPermissions =
      EnumSet.allOf(GlobalUserPermission.class).stream()
          .collect(Collectors.toMap(GlobalUserPermission::name, e -> e));

  private RequiredPermissions resolvePermission(String permission) {

    List<String> permissionStrings = Arrays.asList(permission.split("\\s*\\|\\s*"));
    return new RequiredPermissions(
        enumSet(
            AllocationPermission.class,
            permissionStrings.stream()
                .filter(allocationPermissions::containsKey)
                .map(allocationPermissions::get)
                .collect(Collectors.toSet())),
        enumSet(
            GlobalUserPermission.class,
            permissionStrings.stream()
                .filter(globalPermissions::containsKey)
                .map(globalPermissions::get)
                .collect(Collectors.toSet())));
  }

  private <E extends Enum<E>> EnumSet<E> enumSet(Class<E> clazz, Set<E> set) {
    return set.isEmpty() ? EnumSet.noneOf(clazz) : EnumSet.copyOf(set);
  }
}
