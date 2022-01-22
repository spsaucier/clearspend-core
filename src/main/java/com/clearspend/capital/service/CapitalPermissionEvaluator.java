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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
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
      // we need a method that has a parameterized type argument or return value to
      // check the parameterized type.
      // https://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list
      //noinspection unchecked
      allocationId = (TypedId<AllocationId>) targetDomainObject;
    }
    if (businessId != null && !CurrentUser.get().businessId().equals(businessId)) {
      return false;
    }
    UserRolesAndPermissions userPermissions =
        rolesAndPermissionsService.getUserRolesAndPermissionsForAllocation(allocationId);

    if (userPermissions == null) {
      return false;
    }

    RequiredPermissions overlapPermissions = resolvePermission(String.valueOf(permission));

    overlapPermissions.allocationPermisions.retainAll(userPermissions.allocationPermissions());
    overlapPermissions.globalUserPermissions.retainAll(userPermissions.globalUserPermissions());

    return !overlapPermissions.allocationPermisions.isEmpty()
        || !overlapPermissions.globalUserPermissions.isEmpty();
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    // Deferring implementation until we have a reason to call it
    throw new NotImplementedException();
  }

  private record RequiredPermissions(
      EnumSet<AllocationPermission> allocationPermisions,
      EnumSet<GlobalUserPermission> globalUserPermissions) {}

  private static final Map<String, AllocationPermission> allocationPermisisons =
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
                .filter(allocationPermisisons::containsKey)
                .map(allocationPermisisons::get)
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
