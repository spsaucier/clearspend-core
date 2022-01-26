package com.clearspend.capital.controller.type.security;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserAllocationRoleId;
import com.clearspend.capital.controller.type.user.UserData;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UserAllocationRole {

  /** Null for new or root allocation owner / business owner */
  @JsonProperty("userAllocationRoleId")
  private TypedId<UserAllocationRoleId> userAllocationRoleId;

  /** The allocation owning this record (which may be an ancestor of the allocation queried) */
  @JsonProperty("allocationId")
  @NonNull
  @NotNull(message = "allocationId required")
  private TypedId<AllocationId> allocationId;

  @NonNull
  @JsonProperty("user")
  private UserData user;

  @NonNull
  @JsonProperty("role")
  private String role;

  /** True iff this record comes from an ancestor rather than the queried allocation */
  @JsonProperty("inherited")
  private boolean inherited;

  public UserAllocationRole(UserRolesAndPermissions permissions) {
    userAllocationRoleId = permissions.userAllocationRoleId();
    role = permissions.allocationRole();
    allocationId = permissions.allocationId();
    user =
        new UserData(
            permissions.userId(),
            permissions.userType(),
            permissions.firstName(),
            permissions.lastName());
    inherited = permissions.inherited();
  }
}
