package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CapitalPermissionEvaluatorTest {

  @Test
  void testUniqueNames() {
    // CapitalPermissionEvaluator assumes that the permissions are unique across
    // the AllocationPermission and GlobalUserPermissions scopes, so let's check that.
    final EnumSet<AllocationPermission> allocationPermissions =
        EnumSet.allOf(AllocationPermission.class);
    Set<String> allPerms =
        allocationPermissions.stream().map(AllocationPermission::name).collect(Collectors.toSet());
    final EnumSet<GlobalUserPermission> globalUserPermissions =
        EnumSet.allOf(GlobalUserPermission.class);
    allPerms.addAll(
        globalUserPermissions.stream().map(GlobalUserPermission::name).collect(Collectors.toSet()));
    assertEquals(allocationPermissions.size() + globalUserPermissions.size(), allPerms.size());
  }
}
