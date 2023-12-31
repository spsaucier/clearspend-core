package com.clearspend.capital.controller.type.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class AllocationRolePermissionsResponse {

  @JsonProperty("userAllocationRoleList")
  @NonNull
  @NotNull(message = "userAllocationRoles required")
  List<AllocationRolePermissionRecord> allocationRolePermissionRecords;
}
