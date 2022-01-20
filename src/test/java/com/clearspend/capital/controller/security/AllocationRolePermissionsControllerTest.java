package com.clearspend.capital.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.security.AllocationRolePermissionRecord;
import com.clearspend.capital.controller.type.security.AllocationRolePermissionsResponse;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.service.AllocationService;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class AllocationRolePermissionsControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final AllocationService allocationService;
  private final EntityManager entityManager;

  @SneakyThrows
  @Test
  void getDefaultRoles() {
    testHelper.init();
    final Allocation rootAllocation =
        allocationService.getRootAllocation(testHelper.retrieveBusiness().getId()).allocation();
    final User rootAllocationOwner =
        entityManager.getReference(User.class, rootAllocation.getOwnerId());

    entityManager.flush();
    testHelper.login(rootAllocationOwner); // Has Admin permissions by default

    MockHttpServletResponse response =
        mvc.perform(
                get("/allocation-role-permissions/business/%s"
                        .formatted(rootAllocation.getId().toUuid().toString()))
                    .contentType("application/json")
                    .cookie(testHelper.getDefaultAuthCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());

    AllocationRolePermissionsResponse permissionsResponse =
        objectMapper.readValue(
            response.getContentAsString(), AllocationRolePermissionsResponse.class);

    Map<String, AllocationRolePermissionRecord> permissions =
        permissionsResponse.getAllocationRolePermissionRecords().stream()
            .collect(Collectors.toMap(perms -> perms.getRole_name(), perms -> perms));

    assertEquals(
        permissionsResponse.getAllocationRolePermissionRecords().size(), permissions.size());
    assertEquals(3, permissions.size());
    assertEquals(Set.of("Admin", "Manager", "View only"), permissions.keySet());
    assertEquals(
        EnumSet.allOf(AllocationPermission.class),
        EnumSet.copyOf(Arrays.asList(permissions.get("Admin").getPermissions())));
  }
}
