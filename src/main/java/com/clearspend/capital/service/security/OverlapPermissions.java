package com.clearspend.capital.service.security;

import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.EnumSet;

public record OverlapPermissions(
    EnumSet<AllocationPermission> allocationPermissions,
    EnumSet<GlobalUserPermission> globalUserPermissions) {}
