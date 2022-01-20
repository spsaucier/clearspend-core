package com.clearspend.capital.controller.type.security;

import com.clearspend.capital.common.typedid.data.AllocationRolePermissionsId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class AllocationRolePermissionRecord {

  @JsonProperty("id")
  private final TypedId<AllocationRolePermissionsId> id;

  @JsonProperty("businessId")
  private final TypedId<BusinessId> business_id;

  @JsonProperty("role_name")
  @NonNull
  @NotNull(message = "role_name is required")
  private final String role_name;

  @JsonProperty("permissions")
  @NonNull
  @NotNull(message = "permissions are required")
  private final AllocationPermission[] permissions;

  public AllocationRolePermissionRecord(AllocationRolePermissions permissions) {
    this.permissions = permissions.getPermissions();
    this.role_name = permissions.getRoleName();
    this.business_id = permissions.getBusinessId();
    this.id = permissions.getId();
  }
}
