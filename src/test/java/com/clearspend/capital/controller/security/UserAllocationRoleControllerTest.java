package com.clearspend.capital.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.security.UserAllocationRolesResponse;
import com.clearspend.capital.controller.type.security.UserRolesAndPermissionsRecord;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.security.DefaultRoles;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class UserAllocationRoleControllerTest extends BaseCapitalTest implements DefaultRoles {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final EntityManager entityManager;

  @SneakyThrows
  @Test
  void createOrUpdateRole_ThenDelete() {
    CreateBusinessRecord createBusinessRecord = testHelper.init();
    final Allocation rootAllocation = createBusinessRecord.allocationRecord().allocation();
    final User rootAllocationOwner = createBusinessRecord.user();
    testHelper.setCurrentUser(rootAllocationOwner);
    User manager = testHelper.createUser(createBusinessRecord.business()).user();

    entityManager.flush();
    testHelper.login(rootAllocationOwner); // Has Admin permissions by default
    // Set a permission on the manager
    mvc.perform(
            put("/user-allocation-roles/allocation/%s/user/%s"
                    .formatted(
                        rootAllocation.getId().toUuid().toString(),
                        manager.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .content(ALLOCATION_MANAGER)
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();

    // Get back 2 permissions, one default for the owner and the other for the manager
    entityManager.flush();
    Map<TypedId<UserId>, UserRolesAndPermissionsRecord> permissions =
        getPermissions(rootAllocation);

    assertEquals(ALLOCATION_MANAGER, permissions.get(manager.getId()).getAllocationRole());
    assertEquals(
        ALLOCATION_ADMIN, permissions.get(rootAllocation.getOwnerId()).getAllocationRole());

    // Now edit the permission - give the second user only read permission
    mvc.perform(
            put("/user-allocation-roles/allocation/%s/user/%s"
                    .formatted(
                        rootAllocation.getId().toUuid().toString(),
                        manager.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .content(ALLOCATION_VIEW_ONLY)
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();
    entityManager.flush();

    // And check it
    permissions = getPermissions(rootAllocation);
    assertEquals(ALLOCATION_VIEW_ONLY, permissions.get(manager.getId()).getAllocationRole());
    assertEquals(
        ALLOCATION_ADMIN, permissions.get(rootAllocation.getOwnerId()).getAllocationRole());

    // Then delete the manager user's permission
    mvc.perform(
            delete(
                    "/user-allocation-roles/allocation/%s/user/%s"
                        .formatted(
                            rootAllocation.getId().toUuid().toString(),
                            manager.getId().toUuid().toString()))
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(testHelper.getDefaultAuthCookie()))
        .andExpect(status().is2xxSuccessful())
        .andReturn()
        .getResponse();
    entityManager.flush();

    // And check it
    permissions = getPermissions(rootAllocation);
    assertEquals(
        ALLOCATION_ADMIN, permissions.get(rootAllocation.getOwnerId()).getAllocationRole());
    assertFalse(permissions.containsKey(manager.getId()));
  }

  @NotNull
  private Map<TypedId<UserId>, UserRolesAndPermissionsRecord> getPermissions(
      Allocation rootAllocation) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                get("/user-allocation-roles/allocation/%s"
                        .formatted(rootAllocation.getId().toUuid().toString()))
                    .contentType("application/json")
                    .cookie(testHelper.getDefaultAuthCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    log.info(response.getContentAsString());

    UserAllocationRolesResponse allocationRolesResponse =
        objectMapper.readValue(response.getContentAsString(), UserAllocationRolesResponse.class);

    return allocationRolesResponse.getUserRolesAndPermissionsRecordList().stream()
        .collect(Collectors.toMap(e -> e.getUser().getUserId(), e -> e));
  }
}
