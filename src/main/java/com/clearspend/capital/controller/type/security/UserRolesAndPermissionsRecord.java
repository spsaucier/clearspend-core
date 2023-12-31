package com.clearspend.capital.controller.type.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.user.UserData;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRolesAndPermissionsRecord {

  /** The allocation owning this record (which may be an ancestor of the allocation queried) */
  @JsonProperty("allocationId")
  @NonNull
  @NotNull(message = "allocationId required")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("parentAllocationId")
  private TypedId<AllocationId> parentAllocationId;

  @NonNull
  @JsonProperty("user")
  private UserData user;

  @JsonProperty("allocationRole")
  private String allocationRole;

  /** True iff this record comes from an ancestor rather than the queried allocation */
  @JsonProperty("inherited")
  private boolean inherited;

  @JsonProperty("allocationPermissions")
  @NonNull
  private List<AllocationPermission> allocationPermissions;

  @JsonProperty("globalUserPermissions")
  @NonNull
  private List<GlobalUserPermission> globalUserPermissions;

  public UserRolesAndPermissionsRecord(UserRolesAndPermissions permissions) {
    allocationRole = permissions.allocationRole();
    allocationId = permissions.allocationId();
    parentAllocationId = permissions.parentAllocationId();
    user =
        new UserData(
            permissions.userId(),
            permissions.userType(),
            permissions.firstName(),
            permissions.lastName(),
            false);
    inherited = permissions.inherited();
    allocationPermissions = List.copyOf(permissions.allocationPermissions());
    globalUserPermissions = List.copyOf(permissions.globalUserPermissions());
  }
}
