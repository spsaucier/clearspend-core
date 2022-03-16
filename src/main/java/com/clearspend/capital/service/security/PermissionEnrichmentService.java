package com.clearspend.capital.service.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.service.RolesAndPermissionsService;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionEnrichmentService {

  private final RolesAndPermissionsService rolesAndPermissionsService;

  public boolean evaluatePermission(
      Authentication authentication,
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      String permissions) {
    UserRolesAndPermissions userPermissions =
        getPermissions(authentication, allocationId, businessId);

    if (userPermissions == null
        || (userPermissions.allocationPermissions().isEmpty()
            && userPermissions.globalUserPermissions().isEmpty())) {
      return false;
    }

    RequiredPermissions overlapPermissions = resolvePermission(String.valueOf(permissions));

    overlapPermissions.allocationPermissions.retainAll(userPermissions.allocationPermissions());
    overlapPermissions.globalUserPermissions.retainAll(userPermissions.globalUserPermissions());

    final boolean hasPermission =
        !overlapPermissions.allocationPermissions.isEmpty()
            || !overlapPermissions.globalUserPermissions.isEmpty();
    return hasPermission;
  }

  private UserRolesAndPermissions getPermissions(
      Authentication authentication,
      final TypedId<AllocationId> allocationId,
      final TypedId<BusinessId> businessId) {

    // BusinessID alone implies root allocationID
    // AllocationID could refer to root allocation, but certainly refers to an allocation
    // cache in CapitalAuthenticationConverter
    Object details = authentication.getDetails();
    UserRolesAndPermissionsCache cache = (UserRolesAndPermissionsCache) details;

    return Optional.ofNullable(allocationId)
        .map(
            a ->
                cache
                    .getPermissionsForAllocation(a)
                    .orElse(
                        cache.cachePermissions(
                            rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(a))))
        // wasn't allocation, so looking for root allocation of the business
        .orElseGet(
            () ->
                cache
                    .getPermissionsForBusiness(businessId)
                    .orElse(
                        cache.cachePermissions(
                            rolesAndPermissionsService.getUserRolesAndPermissionsAtRootAllocation(
                                businessId))));
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