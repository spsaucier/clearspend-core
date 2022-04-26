package com.clearspend.capital.service.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;

public record FailedPermissions(
    @NonNull PermissionEvaluationContext permissionEvaluationContext,
    @NonNull String requiredPermissions,
    @Nullable List<UserRolesAndPermissions> userPermissions) {

  private static final String EMPTY_ACTUAL_PERMISSIONS = "[]";
  private static final String EMPTY_FOUND_USER_PERMISSIONS = "UserPermissions[NONE]";

  private String actualPermissionsReducer(final String permissionA, final String permissionB) {
    if (EMPTY_ACTUAL_PERMISSIONS.equals(permissionA)
        && EMPTY_ACTUAL_PERMISSIONS.equals(permissionB)) {
      return EMPTY_ACTUAL_PERMISSIONS;
    } else if (EMPTY_ACTUAL_PERMISSIONS.equals(permissionA)) {
      return permissionB;
    }
    return "%s,%s".formatted(permissionA, permissionB);
  }

  private String foundUserPermissionsReducer(final String permissionA, final String permissionB) {
    if (EMPTY_FOUND_USER_PERMISSIONS.equals(permissionA)
        && EMPTY_FOUND_USER_PERMISSIONS.equals(permissionB)) {
      return EMPTY_FOUND_USER_PERMISSIONS;
    } else if (EMPTY_FOUND_USER_PERMISSIONS.equals(permissionA)) {
      return permissionB;
    }
    return "%s%n    %s".formatted(permissionA, permissionB);
  }

  public String composeFailureMessage(final int index) {
    final String foundUserPermissions =
        Optional.ofNullable(userPermissions).stream()
            .flatMap(List::stream)
            .map(UserPermissions::fromUserRolesAndPermissions)
            .map(UserPermissions::toString)
            .reduce(EMPTY_FOUND_USER_PERMISSIONS, this::foundUserPermissionsReducer);
    final String actualPermissions =
        Optional.ofNullable(userPermissions).stream()
            .flatMap(List::stream)
            .map(this::getActualPermissions)
            .reduce(EMPTY_ACTUAL_PERMISSIONS, this::actualPermissionsReducer);
    final String failureRootMessage =
        "Failure %d. Expected One Of [%s], Actual %s"
            .formatted(index, requiredPermissions, actualPermissions);
    return """
                %s
                    %s
                    %s
                """
        .formatted(failureRootMessage, permissionEvaluationContext, foundUserPermissions)
        .trim();
  }

  private String getActualPermissions(final UserRolesAndPermissions permissions) {
    final Stream<String> allocationPermissionStream =
        permissions.allocationPermissions().stream().map(AllocationPermission::name);
    final Stream<String> globalPermissionsStream =
        permissions.globalUserPermissions().stream().map(GlobalUserPermission::name);
    return Stream.concat(allocationPermissionStream, globalPermissionsStream)
        .collect(Collectors.joining("|", "[", "]"));
  }

  private record UserPermissions(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      boolean isRoot,
      String allocationRole,
      EnumSet<AllocationPermission> allocationPermissions,
      EnumSet<GlobalUserPermission> globalPermissions) {
    public static UserPermissions fromUserRolesAndPermissions(
        final UserRolesAndPermissions userPermissions) {
      return new UserPermissions(
          userPermissions.businessId(),
          userPermissions.allocationId(),
          userPermissions.parentAllocationId() == null,
          userPermissions.allocationRole(),
          userPermissions.allocationPermissions(),
          userPermissions.globalUserPermissions());
    }
  }
}
