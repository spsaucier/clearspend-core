package com.clearspend.capital.service.security;

import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.EnumSet;

public record OverlapPermissions(
    EnumSet<AllocationPermission> allocationPermissions,
    EnumSet<GlobalUserPermission> globalUserPermissions) {

  public OverlapPermissions() {
    this(EnumSet.noneOf(AllocationPermission.class), EnumSet.noneOf(GlobalUserPermission.class));
  }

  public OverlapPermissions copy() {
    return new OverlapPermissions(
        EnumSet.copyOf(allocationPermissions), EnumSet.copyOf(globalUserPermissions));
  }

  public boolean hasPermissions() {
    return !(allocationPermissions().isEmpty() && globalUserPermissions().isEmpty());
  }
}
