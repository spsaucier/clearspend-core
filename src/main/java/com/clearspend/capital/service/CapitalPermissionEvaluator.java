package com.clearspend.capital.service;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.service.type.CurrentUser;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

@Slf4j
public class CapitalPermissionEvaluator implements PermissionEvaluator {

  private final RolesAndPermissionsService rolesAndPermissionsService;

  public CapitalPermissionEvaluator(RolesAndPermissionsService rolesAndPermissionsService) {
    this.rolesAndPermissionsService = rolesAndPermissionsService;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    TypedId<AllocationId> allocationId = null;
    TypedId<BusinessId> businessId = null;
    if (targetDomainObject instanceof Business) {
      businessId = ((Business) targetDomainObject).getId();
    } else if (targetDomainObject instanceof Allocation) {
      allocationId = ((Allocation) targetDomainObject).getId();
    } else if (targetDomainObject instanceof TypedId) {
      // only allocationID will work here, else use the syntax with an explicit targetType
      //noinspection unchecked
      allocationId = (TypedId<AllocationId>) targetDomainObject;
    }

    if (businessId == null) {
      businessId = CurrentUser.getBusinessId();
    }

    return hasPermission(
        authentication,
        allocationId == null ? businessId : allocationId,
        allocationId == null ? "BusinessId" : "AllocationId",
        permission);
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    TypedId<AllocationId> allocationId =
        targetType.equals("AllocationId") ? (TypedId<AllocationId>) targetId : null;
    TypedId<BusinessId> businessId =
        targetType.equals("BusinessId") ? (TypedId<BusinessId>) targetId : null;

    UserRolesAndPermissions userPermissions =
        allocationId == null
            ? rolesAndPermissionsService.getUserRolesAndPermissionsAtRootAllocation(businessId)
            : rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(allocationId);

    if (userPermissions == null
        || (userPermissions.allocationPermissions().isEmpty()
            && userPermissions.globalUserPermissions().isEmpty())) {
      return false;
    }

    RequiredPermissions overlapPermissions = resolvePermission(String.valueOf(permission));

    overlapPermissions.allocationPermissions.retainAll(userPermissions.allocationPermissions());
    overlapPermissions.globalUserPermissions.retainAll(userPermissions.globalUserPermissions());

    return !overlapPermissions.allocationPermissions.isEmpty()
        || !overlapPermissions.globalUserPermissions.isEmpty();
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
