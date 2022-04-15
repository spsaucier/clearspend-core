package com.clearspend.capital.service.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;

public record FailedPermissions(
    @NonNull PermissionEvaluationIds permissionEvaluationIds,
    @NonNull String requiredPermissions,
    @Nullable UserRolesAndPermissions userPermissions) {

  public String composeFailureMessage(final int index) {
    final String foundUserPermissions =
        Optional.ofNullable(userPermissions)
            .map(UserPermissions::fromUserRolesAndPermissions)
            .map(UserPermissions::toString)
            .orElse("UserPermissions[NONE]");
    final String actualPermissions =
        Optional.ofNullable(userPermissions).map(this::getActualPermissions).orElse("[]");
    final String failureRootMessage =
        "Failure %d. Expected One Of [%s], Actual [%s]"
            .formatted(index, requiredPermissions, actualPermissions);
    return """
                %s
                    %s
                    %s
                """
        .formatted(failureRootMessage, permissionEvaluationIds, foundUserPermissions)
        .trim();
  }

  private String getActualPermissions(final UserRolesAndPermissions permissions) {
    final Stream<String> allocationPermissionStream =
        permissions.allocationPermissions().stream().map(AllocationPermission::name);
    final Stream<String> globalPermissionsStream =
        permissions.globalUserPermissions().stream().map(GlobalUserPermission::name);
    return Stream.concat(allocationPermissionStream, globalPermissionsStream)
        .collect(Collectors.joining("|"));
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
